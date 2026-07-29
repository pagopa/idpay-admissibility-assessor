package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.PreallocationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.model.InitiativeCountersPreallocations;
import it.gov.pagopa.admissibility.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingRequestEvaluatorServiceTest {

    @Mock
    RuleEngineService ruleEngineService;

    @Mock
    InitiativeCountersRepository initiativeCountersRepository;

    @Mock
    InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    ReactiveMongoTransactionManager transactionManager;

    @InjectMocks
    OnboardingRequestEvaluatorServiceImpl service;

    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setup() {
        onboardingRequest = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .verifies(new ArrayList<>())
                .build();

        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVE")
                .beneficiaryBudgetFixedCents(1_000L)
                .build();
    }


    @Test
    void evaluate_onboardingKo_fromRuleEngine() {

        EvaluationCompletedDTO engineResult =
                EvaluationCompletedDTO.builder()
                        .userId("USERID")
                        .initiativeId("INITIATIVE")
                        .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                        .onboardingRejectionReasons(List.of(OnboardingRejectionReason.builder().build()))
                        .build();

        when(ruleEngineService.applyRules(onboardingRequest, initiativeConfig))
                .thenReturn(engineResult);

        EvaluationDTO result =
                service.evaluate(onboardingRequest, initiativeConfig).block();

        assertEquals(engineResult, result);

        verifyNoInteractions(initiativeCountersRepository);
        verifyNoInteractions(initiativeCountersPreallocationsRepository);
    }

    @Test
    void evaluate_onboardingOk_hasNoSideEffect() {

        when(ruleEngineService.applyRules(onboardingRequest, initiativeConfig))
                .thenReturn(okEvaluation());

        EvaluationDTO result =
                service.evaluate(onboardingRequest, initiativeConfig).block();

        assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK,
                ((EvaluationCompletedDTO) result).getStatus());

        verifyNoInteractions(initiativeCountersRepository);
        verifyNoInteractions(initiativeCountersPreallocationsRepository);
    }


    @Test
    void updateInitiativeBudget_onboardingKo_rollbackTotal() {

        try (MockedStatic<TransactionalOperator> tx =
                     mockStatic(TransactionalOperator.class);
             MockedStatic<Utils> utils =
                     mockStatic(Utils.class)) {

            EvaluationCompletedDTO evaluation =
                    EvaluationCompletedDTO.builder()
                            .userId("USERID")
                            .initiativeId("INITIATIVE")
                            .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                            .build();

            utils.when(() ->
                            Utils.computePreallocationId("USERID", "INITIATIVE"))
                    .thenReturn("USERID_INITIATIVE");

            InitiativeCountersPreallocations preallocation =
                    preallocation(2_000L);

            when(initiativeCountersPreallocationsRepository
                    .findById("USERID_INITIATIVE"))
                    .thenReturn(Mono.just(preallocation));

            when(initiativeCountersPreallocationsRepository
                    .deleteByIdReturningResult("USERID_INITIATIVE"))
                    .thenReturn(Mono.just(true));

            when(initiativeCountersRepository
                    .deallocatedPartialBudget("INITIATIVE", 2_000L))
                    .thenReturn(Mono.just(new InitiativeCounters()));

            TransactionalOperator op = mock(TransactionalOperator.class);
            tx.when(() -> TransactionalOperator.create(transactionManager))
                    .thenReturn(op);

            when(op.transactional(any(Mono.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            StepVerifier.create(
                            service.updateInitiativeBudget(
                                    evaluation, initiativeConfig, onboardingRequest))
                    .expectNext(evaluation)
                    .verifyComplete();
        }
    }

    @Test
    void updateInitiativeBudget_onboardingOk_min_budgetRollbackPartial() {

        initiativeConfig.setBeneficiaryBudgetFixedCents(null);

        onboardingRequest.getVerifies().add(
                new VerifyDTO(
                        "TEST",
                        true,
                        true,
                        "TH",
                        1_000L,
                        2_000L,
                        List.of(OnboardingRejectionReason.builder().build())
                )
        );

        EvaluationCompletedDTO evaluation = okEvaluation();

        try (MockedStatic<TransactionalOperator> tx =
                     mockStatic(TransactionalOperator.class);
             MockedStatic<Utils> utils =
                     mockStatic(Utils.class)) {

            utils.when(() ->
                            Utils.computePreallocationId("USERID", "INITIATIVE"))
                    .thenReturn("USERID_INITIATIVE");

            InitiativeCountersPreallocations preallocation =
                    preallocation(2_000L);

            when(initiativeCountersPreallocationsRepository
                    .findById("USERID_INITIATIVE"))
                    .thenReturn(Mono.just(preallocation));

            when(initiativeCountersPreallocationsRepository
                    .updatePreallocatedAmount("USERID_INITIATIVE", 1_000L))
                    .thenReturn(Mono.just(true));

            when(initiativeCountersRepository
                    .deallocatedPartialBudget("INITIATIVE", 1_000L))
                    .thenReturn(Mono.just(new InitiativeCounters()));

            TransactionalOperator op = mock(TransactionalOperator.class);
            tx.when(() -> TransactionalOperator.create(transactionManager))
                    .thenReturn(op);

            when(op.transactional(any(Mono.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            StepVerifier.create(
                            service.updateInitiativeBudget(
                                    evaluation, initiativeConfig, onboardingRequest))
                    .expectNext(evaluation)
                    .verifyComplete();
        }
    }


    private EvaluationCompletedDTO okEvaluation() {
        return EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .onboardingRejectionReasons(new ArrayList<>())
                .build();
    }

    private InitiativeCountersPreallocations preallocation(long amount) {
        return InitiativeCountersPreallocations.builder()
                .id("USERID_INITIATIVE")
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .status(PreallocationStatus.PREALLOCATED)
                .preallocatedAmountCents(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}