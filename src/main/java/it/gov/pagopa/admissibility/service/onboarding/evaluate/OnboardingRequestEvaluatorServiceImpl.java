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

    @Override
    public Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest,
                                        InitiativeConfig initiativeConfig) {

        EvaluationDTO evaluation = ruleEngineService.applyRules(
                onboardingRequest, initiativeConfig
        );

        if (!(evaluation instanceof EvaluationCompletedDTO completed)) {
            return Mono.just(evaluation);
        }

        if (!OnboardingEvaluationStatus.ONBOARDING_OK.equals(completed.getStatus())) {
            log.info(
                    "[ONBOARDING_KO][RULE_ENGINE] user={} initiative={}",
                    onboardingRequest.getUserId(),
                    onboardingRequest.getInitiativeId()
            );
            return Mono.just(completed);
        }

        // ✅ Calcolo budget beneficiario
        calculateBeneficiaryBudget(
                onboardingRequest,
                initiativeConfig,
                completed
        );

        long preallocated = completed.getBeneficiaryBudgetFixedCents();
        long toDeallocate = calculateDeallocatedBudget(
                onboardingRequest,
                initiativeConfig,
                preallocated
        );

        Mono<EvaluationDTO> budgetMono =
                toDeallocate > 0
                        ? initiativeCountersRepository
                        .deallocatedPartialBudget(
                                completed.getInitiativeId(),
                                toDeallocate
                        )
                        .thenReturn(completed)
                        : Mono.just(completed);

        return budgetMono
                .doOnSuccess(res -> onboardingRequest.setBudgetReserved(true))
                .switchIfEmpty(handleBudgetExhausted(completed))
                .map(EvaluationDTO.class::cast);
    }

    private Mono<EvaluationDTO> handleBudgetExhausted(
            EvaluationCompletedDTO completed) {

        log.info(
                "[ONBOARDING_KO][BUDGET_EXHAUSTED] initiative={}",
                completed.getInitiativeId()
        );

        completed.getOnboardingRejectionReasons().add(
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason
                                .OnboardingRejectionReasonType
                                .BUDGET_EXHAUSTED)
                        .code(OnboardingConstants
                                .REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                        .build()
        );

        completed.setStatus(OnboardingEvaluationStatus.ONBOARDING_KO);
        return Mono.just(completed);
    }

    /**
     * Calcola quanto budget va rilasciato rispetto alla preallocazione iniziale
     */
    private long calculateDeallocatedBudget(OnboardingDTO onboardingRequest,
                                            InitiativeConfig initiativeConfig,
                                            long finalBudget) {

        // Budget fisso → nessuna riallocazione
        if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
            return initiativeConfig.getBeneficiaryBudgetFixedCents() - finalBudget;
        }

        // Budget variabile → unica Verify con MAX
        for (VerifyDTO verify : onboardingRequest.getVerifies()) {
            if (verify.getBeneficiaryBudgetCentsMax() != null) {
                return verify.getBeneficiaryBudgetCentsMax() - finalBudget;
            }
        }

        return 0;
    }

    /**
     * Metodo già validato in precedenza
     */
    private void calculateBeneficiaryBudget(OnboardingDTO onboardingRequest,
                                            InitiativeConfig initiativeConfig,
                                            EvaluationCompletedDTO result) {

        if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
            result.setBeneficiaryBudgetFixedCents(
                    initiativeConfig.getBeneficiaryBudgetFixedCents()
            );
            return;
        }

        for (VerifyDTO verify : onboardingRequest.getVerifies()) {

            if (verify.getBeneficiaryBudgetCentsMax() == null) {
                continue;
            }

            if (!verify.isVerify()) {
                result.setBeneficiaryBudgetFixedCents(
                        verify.getBeneficiaryBudgetCentsMax()
                );
                return;
            }

            onboardingRequest.getResultsVerifies().stream()
                    .filter(rv -> verify.getCode().equals(rv.getCode()))
                    .findFirst()
                    .ifPresentOrElse(
                            rv -> result.setBeneficiaryBudgetFixedCents(
                                    rv.isResultVerify()
                                            ? verify.getBeneficiaryBudgetCentsMax()
                                            : verify.getBeneficiaryBudgetCentsMin()
                            ),
                            () -> result.setBeneficiaryBudgetFixedCents(
                                    verify.getBeneficiaryBudgetCentsMin()
                            )
                    );
            return;
        }

        throw new IllegalStateException(
                "Unable to calculate beneficiary budget"
        );
    }

    @Override
    public Mono<EvaluationDTO> updateInitiativeBudget(EvaluationDTO evaluationDTO,
                                                      InitiativeConfig initiativeConfig) {

        if (!(evaluationDTO instanceof EvaluationCompletedDTO completed)) {
            return Mono.just(evaluationDTO);
        }

        if (!OnboardingEvaluationStatus.ONBOARDING_KO.equals(completed.getStatus())
                && !OnboardingEvaluationStatus.JOINED.equals(completed.getStatus())) {
            return Mono.just(evaluationDTO);
        }

        TransactionalOperator tx =
                TransactionalOperator.create(transactionManager);

        long deallocate = completed.getBeneficiaryBudgetFixedCents();

        return tx.transactional(
                initiativeCountersPreallocationsRepository
                        .deleteByIdReturningResult(
                                Utils.computePreallocationId(
                                        completed.getUserId(),
                                        completed.getInitiativeId()
                                )
                        )
                        .filter(Boolean::booleanValue)
                        .flatMap(ok ->
                                initiativeCountersRepository
                                        .deallocatedPartialBudget(
                                                completed.getInitiativeId(),
                                                deallocate
                                        )
                        )
                        .thenReturn(evaluationDTO)
        );
    }
}
