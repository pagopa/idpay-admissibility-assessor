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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingRequestEvaluatorServiceTest {

    @Mock
    private RuleEngineService ruleEngineService;
    @Mock
    private InitiativeCountersRepository initiativeCountersRepository;
    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    @Mock
    private ReactiveMongoTransactionManager transactionManager;

    @InjectMocks
    private OnboardingRequestEvaluatorServiceImpl service;

    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setUp() {
        onboardingRequest = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .verifies(new ArrayList<>())
                .build();

        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVE")
                .initiativeBudgetCents(10_000L)
                .beneficiaryBudgetFixedCents(1_000L)
                .build();
    }

    @Test
    void rejectedFromRuleEngine() {

        EvaluationCompletedDTO engineResult =
                EvaluationCompletedDTO.builder()
                        .initiativeId("INITIATIVE")
                        .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                        .onboardingRejectionReasons(List.of(
                                new OnboardingRejectionReason(
                                        OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR,
                                        "DUMMY",
                                        null, null, null)))
                        .build();

        when(ruleEngineService.applyRules(onboardingRequest, initiativeConfig))
                .thenReturn(engineResult);

        EvaluationDTO result =
                service.evaluate(onboardingRequest, initiativeConfig).block();

        Assertions.assertEquals(engineResult, result);
        verifyNoInteractions(initiativeCountersRepository);
    }

    @Test
    void onboardingOk_fixedBudget() {

        mockRuleEngineOk();

        when(initiativeCountersRepository
                .deallocatedPartialBudget(any(), anyLong()))
                .thenReturn(Mono.just(new InitiativeCounters()));

        EvaluationCompletedDTO result =
                (EvaluationCompletedDTO) service
                        .evaluate(onboardingRequest, initiativeConfig)
                        .block();

        Assertions.assertEquals(
                initiativeConfig.getBeneficiaryBudgetFixedCents(),
                result.getBeneficiaryBudgetCents()
        );

        verify(initiativeCountersRepository)
                .deallocatedPartialBudget("INITIATIVE", 0L);
    }

    @Test
    void onboardingOk_variableBudget_max() {

        initiativeConfig.setBeneficiaryBudgetFixedCents(null);

        onboardingRequest.getVerifies().add(
                new VerifyDTO(
                        "ISEE_THRESHOLD",
                        true,
                        true,
                        "TH_CODE",
                        1_000L,
                        2_000L,
                        Collections.emptyList() // esito OK → MAX
                )
        );

        mockRuleEngineOk();

        when(initiativeCountersRepository
                .deallocatedPartialBudget(any(), anyLong()))
                .thenReturn(Mono.just(new InitiativeCounters()));

        EvaluationCompletedDTO result =
                (EvaluationCompletedDTO) service
                        .evaluate(onboardingRequest, initiativeConfig)
                        .block();

        Assertions.assertEquals(2_000L, result.getBeneficiaryBudgetCents());
    }

    @Test
    void onboardingOk_variableBudget_min() {

        initiativeConfig.setBeneficiaryBudgetFixedCents(null);

        onboardingRequest.getVerifies().add(
                new VerifyDTO(
                        "ISEE_THRESHOLD",
                        true,
                        true,
                        "TH_CODE",
                        1_000L,
                        2_000L,
                        List.of(OnboardingRejectionReason.builder().build()) // esito KO → MIN
                )
        );

        mockRuleEngineOk();

        when(initiativeCountersRepository
                .deallocatedPartialBudget(any(), anyLong()))
                .thenReturn(Mono.just(new InitiativeCounters()));

        EvaluationCompletedDTO result =
                (EvaluationCompletedDTO) service
                        .evaluate(onboardingRequest, initiativeConfig)
                        .block();

        Assertions.assertEquals(1_000L, result.getBeneficiaryBudgetCents());
    }

    @Test
    void updateInitiativeBudget_onboardingKo() {

        EvaluationCompletedDTO completed =
                EvaluationCompletedDTO.builder()
                        .userId("USERID")
                        .initiativeId("INITIATIVE")
                        .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                        .build();

        InitiativeCounters counters = new InitiativeCounters();

        InitiativeCountersPreallocations preallocation =
                InitiativeCountersPreallocations.builder()
                        .id("USERID_INITIATIVE")
                        .userId("USERID")
                        .initiativeId("INITIATIVE")
                        .preallocatedAmountCents(1_000L)
                        .status(PreallocationStatus.PREALLOCATED)
                        .createdAt(LocalDateTime.now())
                        .build();

        Mockito.when(
                        initiativeCountersPreallocationsRepository
                                .findById(anyString()))
                .thenReturn(Mono.just(preallocation));

        Mockito.when(
                        initiativeCountersPreallocationsRepository
                                .deleteByIdReturningResult(anyString()))
                .thenReturn(Mono.just(true));

        Mockito.when(
                        initiativeCountersRepository
                                .deallocatedPartialBudget(any(), anyLong()))
                .thenReturn(Mono.just(counters));

        try (MockedStatic<TransactionalOperator> tx =
                     Mockito.mockStatic(TransactionalOperator.class);
             MockedStatic<Utils> utils =
                     Mockito.mockStatic(Utils.class)) {

            utils.when(() ->
                            Utils.computePreallocationId("USERID", "INITIATIVE"))
                    .thenReturn("USERID_INITIATIVE");

            TransactionalOperator op = mock(TransactionalOperator.class);
            tx.when(() -> TransactionalOperator.create(transactionManager))
                    .thenReturn(op);

            when(op.transactional(any(Mono.class)))
                    .thenAnswer(i -> i.getArgument(0));

            StepVerifier.create(
                            service.updateInitiativeBudget(completed, initiativeConfig))
                    .expectNext(completed)
                    .verifyComplete();
        }
    }

    private void mockRuleEngineOk() {
        when(ruleEngineService.applyRules(onboardingRequest, initiativeConfig))
                .thenReturn(
                        EvaluationCompletedDTO.builder()
                                .initiativeId("INITIATIVE")
                                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                                .onboardingRejectionReasons(new ArrayList<>())
                                .build()
                );
    }
}


