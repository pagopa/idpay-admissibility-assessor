package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class OnboardingRequestEvaluatorServiceImpl
        implements OnboardingRequestEvaluatorService {

    private final RuleEngineService ruleEngineService;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final ReactiveMongoTransactionManager transactionManager;

    public OnboardingRequestEvaluatorServiceImpl(
            RuleEngineService ruleEngineService,
            InitiativeCountersRepository initiativeCountersRepository,
            InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
            ReactiveMongoTransactionManager transactionManager) {

        this.ruleEngineService = ruleEngineService;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.transactionManager = transactionManager;
    }

    /**
     * Valuta la richiesta di onboarding:
     * - applica il rule engine
     * - determina SOLO l’esito (OK / KO / JOINED / etc.)
     * - NON gestisce il budget
     */
    @Override
    public Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest,
                                        InitiativeConfig initiativeConfig) {

        EvaluationDTO result =
                ruleEngineService.applyRules(onboardingRequest, initiativeConfig);

        if (result instanceof EvaluationCompletedDTO completed) {
            if (!OnboardingEvaluationStatus.ONBOARDING_OK.equals(completed.getStatus())) {
                log.info(
                        "[ONBOARDING_REQUEST][RULE_ENGINE_KO] user={} initiative={} reasons={}",
                        onboardingRequest.getUserId(),
                        onboardingRequest.getInitiativeId(),
                        completed.getOnboardingRejectionReasons()
                );
            } else {
                log.trace(
                        "[ONBOARDING_REQUEST][RULE_ENGINE_OK] user={} initiative={}",
                        onboardingRequest.getUserId(),
                        onboardingRequest.getInitiativeId()
                );
            }
        }

        return Mono.just(result);
    }

    /**
     * Gestisce l’allineamento del budget iniziativa a valle della evaluate.
     * Regole:
     * - KO / JOINED  -> rollback totale della preallocazione
     * - OK          -> rollback parziale se finalBudget < preallocated
     * - Nessuna differenza -> nessuna operazione
     */
    @Override
    public Mono<EvaluationDTO> updateInitiativeBudget(EvaluationDTO evaluationDTO,
                                                      InitiativeConfig initiativeConfig,
                                                      OnboardingDTO onboardingRequest) {

        if (!(evaluationDTO instanceof EvaluationCompletedDTO completedDTO)) {
            return Mono.just(evaluationDTO);
        }

        TransactionalOperator tx =
                TransactionalOperator.create(transactionManager);

        String preallocationId = Utils.computePreallocationId(
                completedDTO.getUserId(),
                completedDTO.getInitiativeId()
        );

        return tx.transactional(
                initiativeCountersPreallocationsRepository
                        .findById(preallocationId)
                        .switchIfEmpty(Mono.error(
                                new IllegalStateException(
                                        "Missing preallocation for id " + preallocationId)))
                        .flatMap(preallocation -> {

                            long preallocated =
                                    preallocation.getPreallocatedAmountCents();

                            if (OnboardingEvaluationStatus.ONBOARDING_KO.equals(completedDTO.getStatus())
                                    || OnboardingEvaluationStatus.JOINED.equals(completedDTO.getStatus())) {

                                log.info(
                                        "[ONBOARDING][ROLLBACK_TOTAL] user={} initiative={} amount={}",
                                        completedDTO.getUserId(),
                                        completedDTO.getInitiativeId(),
                                        preallocated
                                );

                                return initiativeCountersPreallocationsRepository
                                        .deleteByIdReturningResult(preallocationId)
                                        .filter(Boolean::booleanValue)
                                        .flatMap(deleted ->
                                                initiativeCountersRepository
                                                        .deallocatedPartialBudget(
                                                                completedDTO.getInitiativeId(),
                                                                preallocated
                                                        ))
                                        .thenReturn(evaluationDTO);
                            }

                            calculateBeneficiaryBudget(
                                    onboardingRequest,
                                    initiativeConfig,
                                    completedDTO
                            );

                            long finalBudget = Optional.ofNullable(
                                    completedDTO.getBeneficiaryBudgetCents()
                            ).orElse(0L);

                            long toDeallocate = preallocated - finalBudget;

                            if (toDeallocate > 0) {
                                log.info(
                                        "[ONBOARDING][DEALLOCATE_PARTIAL] user={} initiative={} preallocated={} final={} deallocated={}",
                                        completedDTO.getUserId(),
                                        completedDTO.getInitiativeId(),
                                        preallocated,
                                        finalBudget,
                                        toDeallocate
                                );


                                return initiativeCountersPreallocationsRepository
                                        .updatePreallocatedAmount(
                                                preallocationId,
                                                finalBudget
                                        )
                                        .then(
                                                initiativeCountersRepository.deallocatedPartialBudget(
                                                        completedDTO.getInitiativeId(),
                                                        toDeallocate
                                                )
                                        )
                                        .thenReturn(evaluationDTO);

                            }

                            log.debug(
                                    "[ONBOARDING][NO_BUDGET_ADJUSTMENT] user={} initiative={} finalBudget={}",
                                    completedDTO.getUserId(),
                                    completedDTO.getInitiativeId(),
                                    finalBudget
                            );

                            return Mono.just(evaluationDTO);
                        })
        );
    }

    /**
     * Calcola il budget finale spettante all’utente.
     * Il valore NON viene mai preallocato da questo servizio.
     */
    private void calculateBeneficiaryBudget(OnboardingDTO onboardingRequest,
                                            InitiativeConfig initiativeConfig,
                                            EvaluationCompletedDTO result) {

        // Budget fisso
        if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
            result.setBeneficiaryBudgetCents(
                    initiativeConfig.getBeneficiaryBudgetFixedCents()
            );
            return;
        }

        // Budget variabile (unico Verify con MAX)
        for (VerifyDTO verify : onboardingRequest.getVerifies()) {

            if (verify.getBeneficiaryBudgetCentsMax() == null) {
                continue;
            }

            // verify = false → OK implicito → MAX
            if (!verify.isVerify()) {
                result.setBeneficiaryBudgetCents(
                        verify.getBeneficiaryBudgetCentsMax()
                );
                return;
            }

            // KO parziale → MIN
            if (!verify.getReasonList().isEmpty()) {
                result.setBeneficiaryBudgetCents(
                        verify.getBeneficiaryBudgetCentsMin()
                );
                return;
            }

            // OK pieno → MAX
            result.setBeneficiaryBudgetCents(
                    verify.getBeneficiaryBudgetCentsMax()
            );
            return;
        }

        throw new IllegalStateException(
                "Unable to calculate beneficiary budget: no fixed budget and no variable verify with max"
        );
    }
}