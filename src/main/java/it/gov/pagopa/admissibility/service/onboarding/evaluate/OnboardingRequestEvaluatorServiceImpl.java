package it.gov.pagopa.admissibility.service.onboarding.evaluate;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class OnboardingRequestEvaluatorServiceImpl implements OnboardingRequestEvaluatorService {

    private final RuleEngineService ruleEngineService;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final ReactiveMongoTransactionManager transactionManager;

    public OnboardingRequestEvaluatorServiceImpl(RuleEngineService ruleEngineService,
                                                 InitiativeCountersRepository initiativeCountersRepository,
                                                 InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
                                                 ReactiveMongoTransactionManager transactionManager) {
        this.ruleEngineService = ruleEngineService;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {

        final EvaluationDTO result = ruleEngineService.applyRules(onboardingRequest, initiativeConfig);

        if (!(result instanceof EvaluationCompletedDTO completed)) {
            return Mono.just(result);
        }

        if (!OnboardingEvaluationStatus.ONBOARDING_OK.equals(completed.getStatus())) {
            log.info("[ONBOARDING_REQUEST][RULE_ENGINE_KO] user={} initiative={} reasons={}",
                    onboardingRequest.getUserId(),
                    onboardingRequest.getInitiativeId(),
                    completed.getOnboardingRejectionReasons());
            return Mono.just(completed);
        }

        log.trace("[ONBOARDING_REQUEST][RULE_ENGINE_OK] user={} initiative={}",
                onboardingRequest.getUserId(),
                onboardingRequest.getInitiativeId());

        // 1) calcolo budget finale (DR: max/min o fixed)
        calculateBeneficiaryBudget(onboardingRequest, initiativeConfig, completed);

        // 2) calcolo deallocazione: preallocated - finalBudget
        long preallocated = calculatePreallocatedAmount(onboardingRequest, initiativeConfig);
        long finalBudget = Optional.ofNullable(completed.getBeneficiaryBudgetCents()).orElse(0L);
        long deallocatedBudget = preallocated - finalBudget;

        return  initiativeCountersRepository
                .deallocatedPartialBudget(completed.getInitiativeId(), deallocatedBudget)
                .map(c -> {
                    log.info("[ONBOARDING_REQUEST][ONBOARDING_OK][BUDGET] user={} initiative={} budgetFinal={} preallocated={} deallocated={}",
                            onboardingRequest.getUserId(),
                            initiativeConfig.getInitiativeId(),
                            finalBudget,
                            preallocated,
                            Math.max(deallocatedBudget, 0));
                    onboardingRequest.setBudgetReserved(true);
                    return completed;
                })
                .map(EvaluationDTO.class::cast);
    }

    /**
     * Se esiste un VerifyDTO con beneficiaryBudgetCentsMax != null => iniziativa a budget variabile
     * altrimenti budget fisso (initiativeConfig.beneficiaryBudgetFixedCents).
     */
    private void calculateBeneficiaryBudget(OnboardingDTO onboardingRequest,
                                            InitiativeConfig initiativeConfig,
                                            EvaluationCompletedDTO result) {

        // Budget fisso iniziativa
        if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
            result.setBeneficiaryBudgetCents(
                    initiativeConfig.getBeneficiaryBudgetFixedCents()
            );
            return;
        }

        // Budget variabile: unico verify con MAX
        for (VerifyDTO verify : onboardingRequest.getVerifies()) {

            if (verify.getBeneficiaryBudgetCentsMax() == null) {
                continue;
            }

            // verify=false -> OK implicito -> MAX (DR-consistente)
            if (!verify.isVerify()) {
                result.setBeneficiaryBudgetCents(
                        verify.getBeneficiaryBudgetCentsMax()
                );
                return;
            }

            // verify=true -> guardo direttamente l'esito
            Boolean verifyResult = verify.getResult();

            // risultato mancante o KO -> MIN (scelta conservativa)
            if (!Boolean.TRUE.equals(verifyResult)) {
                result.setBeneficiaryBudgetCents(
                        verify.getBeneficiaryBudgetCentsMin()
                );
                return;
            }

            // esito OK -> MAX
            result.setBeneficiaryBudgetCents(
                    verify.getBeneficiaryBudgetCentsMax()
            );
            return;
        }

        // 3️Fallback: configurazione incoerente
        throw new IllegalStateException(
                "Unable to calculate beneficiary budget: no fixed budget and no variable verify with max"
        );
    }
    /**
     * Preallocazione attesa:
     * - se budget fisso => fixed
     * - se variabile => MAX dell'unico verify con max != null
     */
    private long calculatePreallocatedAmount(OnboardingDTO onboardingRequest,
                                             InitiativeConfig initiativeConfig) {

        if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
            return initiativeConfig.getBeneficiaryBudgetFixedCents();
        }

        for (VerifyDTO verify : onboardingRequest.getVerifies()) {
            if (verify.getBeneficiaryBudgetCentsMax() != null) {
                return verify.getBeneficiaryBudgetCentsMax();
            }
        }

        return 0L;
    }

    @Override
    public Mono<EvaluationDTO> updateInitiativeBudget(EvaluationDTO evaluationDTO,
                                                      InitiativeConfig initiativeConfig) {

        if (!(evaluationDTO instanceof EvaluationCompletedDTO completedDTO)
                || !(OnboardingEvaluationStatus.ONBOARDING_KO.equals(completedDTO.getStatus())
                || OnboardingEvaluationStatus.JOINED.equals(completedDTO.getStatus()))) {
            return Mono.just(evaluationDTO);
        }

        TransactionalOperator tx = TransactionalOperator.create(transactionManager);

        String preallocateId = Utils.computePreallocationId(
                evaluationDTO.getUserId(),
                evaluationDTO.getInitiativeId()
        );

        return tx.transactional(
                initiativeCountersPreallocationsRepository
                        .findById(preallocateId)
                        .switchIfEmpty(
                                Mono.error(new IllegalStateException(
                                        "Missing preallocation for id " + preallocateId
                                ))
                        )
                        .flatMap(preallocate ->
                                initiativeCountersPreallocationsRepository
                                        .deleteByIdReturningResult(preallocateId)
                                        .filter(Boolean::booleanValue)
                                        .flatMap(deleted ->
                                                initiativeCountersRepository
                                                        .deallocatedPartialBudget(
                                                                completedDTO.getInitiativeId(),
                                                                preallocate.getPreallocatedAmountCents()
                                                        )
                                        )
                        )
                        .thenReturn(evaluationDTO)
        );
    }
}

