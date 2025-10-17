package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
    private OnboardingRequestEvaluatorServiceImpl onboardingRequestEvaluatorService;

    private final OnboardingDTO onboardingRequest = new OnboardingDTO();
    private final InitiativeConfig initiativeConfig = new InitiativeConfig();

    public OnboardingRequestEvaluatorServiceTest(){
        onboardingRequest.setInitiativeId("ID");

        initiativeConfig.setInitiativeBudgetCents(10_00L);
        initiativeConfig.setBeneficiaryInitiativeBudgetCents(1_00L);
        initiativeConfig.setBeneficiaryInitiativeBudgetMaxCents(2_00L);
    }

    @Test
    void testRejectedFromRuleEngine(){
        //given
        final List<OnboardingRejectionReason> ruleEngineMockedRejectionReason = List.of(OnboardingRejectionReason.builder().type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR).code("DUMMY_REJECTION_REASON").build());

        final EvaluationCompletedDTO mockedRuleEngineResult = EvaluationCompletedDTO.builder()
                .initiativeId(onboardingRequest.getInitiativeId())
                .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                .onboardingRejectionReasons(new ArrayList<>(ruleEngineMockedRejectionReason))
                .build();

        Mockito.when(ruleEngineService.applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig))).thenAnswer(i-> mockedRuleEngineResult);

        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertSame(mockedRuleEngineResult, resultCompleted);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, resultCompleted.getStatus());
        Assertions.assertEquals(ruleEngineMockedRejectionReason, resultCompleted.getOnboardingRejectionReasons());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));

        Mockito.verifyNoMoreInteractions(ruleEngineService);
        Mockito.verifyNoInteractions(initiativeCountersRepository);
    }

    private void configureSuccesfulRuleEngine(){
        Mockito.when(ruleEngineService.applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig))).thenAnswer(i-> EvaluationCompletedDTO.builder()
                .initiativeId(((OnboardingDTO)i.getArgument(0)).getInitiativeId())
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .onboardingRejectionReasons(new ArrayList<>())
                .build());
    }

    @Test
    void testSuccessful(){
        //give
        onboardingRequest.setVerifyIsee(false);
        onboardingRequest.setUnderThreshold(false);
        initiativeConfig.setBeneficiaryInitiativeBudgetMaxCents(200_00L);
        initiativeConfig.setIseeThresholdCode("THRESHOLD_CODE");

        configureSuccesfulRuleEngine();

        initiativeConfig.setBeneficiaryInitiativeBudgetCents(10000L);
        initiativeConfig.setBeneficiaryInitiativeBudgetMaxCents(20000L);

        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, resultCompleted.getStatus());
        Assertions.assertEquals(Collections.emptyList(), resultCompleted.getOnboardingRejectionReasons());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));
        Mockito.verify(initiativeCountersRepository, never()).deallocatedPartialBudget(Mockito.any(), Mockito.eq(10000));

        Mockito.verifyNoMoreInteractions(ruleEngineService, initiativeCountersRepository);
    }

    @Test
    void testSuccessfulBudgetMax() {
        //give
        onboardingRequest.setVerifyIsee(true);
        onboardingRequest.setUnderThreshold(true);

        initiativeConfig.setBeneficiaryInitiativeBudgetMaxCents(200_00L);
        initiativeConfig.setIseeThresholdCode("THRESHOLD_CODE");

        configureSuccesfulRuleEngine();


        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, resultCompleted.getStatus());
        Assertions.assertEquals(Collections.emptyList(), resultCompleted.getOnboardingRejectionReasons());
        Assertions.assertEquals(initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents(), ((EvaluationCompletedDTO) result).getBeneficiaryBudgetCents());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));
        Mockito.verify(initiativeCountersRepository,never()).deallocatedPartialBudget(Mockito.any(), Mockito.eq(10000));

        Mockito.verifyNoMoreInteractions(ruleEngineService, initiativeCountersRepository);
    }

    @Test
    void testSuccessfulBudgetMin() {
        //give
        String initiative = "INITIATIVE_ID";
        onboardingRequest.setVerifyIsee(true);
        onboardingRequest.setUnderThreshold(false);
        onboardingRequest.setInitiativeId(initiative);
        initiativeConfig.setInitiativeBudgetCents(1000000L);
        initiativeConfig.setBeneficiaryInitiativeBudgetCents(100_00L);
        initiativeConfig.setBeneficiaryInitiativeBudgetMaxCents(200_00L);
        initiativeConfig.setIseeThresholdCode("THRESHOLD_CODE");
        initiativeConfig.setInitiativeId(initiative);

        configureSuccesfulRuleEngine();

        InitiativeCounters counters = new InitiativeCounters();

        Mockito.when(
                initiativeCountersRepository.deallocatedPartialBudget(Mockito.any(), Mockito.eq(100_00L))
        ).thenReturn(Mono.just(counters));
        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, resultCompleted.getStatus());
        Assertions.assertEquals(Collections.emptyList(), resultCompleted.getOnboardingRejectionReasons());
        Assertions.assertEquals(initiativeConfig.getBeneficiaryInitiativeBudgetCents(), ((EvaluationCompletedDTO) result).getBeneficiaryBudgetCents());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));
        Mockito.verify(initiativeCountersRepository, times(1)).deallocatedPartialBudget(Mockito.any(), Mockito.anyLong());

        Mockito.verifyNoMoreInteractions(ruleEngineService, initiativeCountersRepository);
    }


    @Test
    void updateInitiativeBudget_deallocateOnboardingKO() {
        EvaluationCompletedDTO evaluationDTO = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId(initiativeConfig.getInitiativeId())
                .verifyIsee(false)
                .status(OnboardingEvaluationStatus.ONBOARDING_KO)
                .build();

        InitiativeCounters mockCounter = Mockito.mock(InitiativeCounters.class);

        try (MockedStatic<Utils> utilsMock = Mockito.mockStatic(Utils.class);
             MockedStatic<TransactionalOperator> transactionalOperatorMock = Mockito.mockStatic(TransactionalOperator.class)) {

            utilsMock.when(() -> Utils.computePreallocationId(evaluationDTO.getUserId(), evaluationDTO.getInitiativeId()))
                    .thenReturn(evaluationDTO.getUserId() + "_" + evaluationDTO.getInitiativeId());

            Mockito.when(initiativeCountersPreallocationsRepository.deleteByIdReturningResult(
                            evaluationDTO.getUserId() + "_" + evaluationDTO.getInitiativeId()))
                    .thenReturn(Mono.just(true));

            Mockito.when(initiativeCountersRepository.deallocatedPartialBudget(
                            initiativeConfig.getInitiativeId(),
                            initiativeConfig.getBeneficiaryInitiativeBudgetCents()))
                    .thenReturn(Mono.just(mockCounter));

            TransactionalOperator transactionalOperator = Mockito.mock(TransactionalOperator.class);
            transactionalOperatorMock.when(() -> TransactionalOperator.create(transactionManager))
                    .thenReturn(transactionalOperator);

            Mockito.when(transactionalOperator.transactional(Mockito.any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StepVerifier.create(onboardingRequestEvaluatorService.updateInitiativeBudget(evaluationDTO, initiativeConfig))
                    .expectNext(evaluationDTO)
                    .verifyComplete();

            Mockito.verify(initiativeCountersPreallocationsRepository).deleteByIdReturningResult(Mockito.any());
            Mockito.verify(initiativeCountersRepository).deallocatedPartialBudget(Mockito.any(), Mockito.anyLong());
        }
    }



    @Test
    void updateInitiativeBudget_withJoined() {
        EvaluationCompletedDTO evaluationDTO = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId(initiativeConfig.getInitiativeId())
                .verifyIsee(true)
                .status(OnboardingEvaluationStatus.JOINED)
                .build();

        InitiativeCounters mockCounter = Mockito.mock(InitiativeCounters.class);

        try (MockedStatic<TransactionalOperator> transactionalOperatorMock = Mockito.mockStatic(TransactionalOperator.class)) {

            Mockito.when(initiativeCountersPreallocationsRepository.deleteByIdReturningResult(Utils.computePreallocationId(evaluationDTO.getUserId(), evaluationDTO.getInitiativeId())))
                    .thenReturn(Mono.just(true));

            Mockito.when(initiativeCountersRepository.deallocatedPartialBudget(initiativeConfig.getInitiativeId(), initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents()))
                    .thenReturn(Mono.just(mockCounter));

            TransactionalOperator transactionalOperator = Mockito.mock(TransactionalOperator.class);
            transactionalOperatorMock.when(() -> TransactionalOperator.create(transactionManager))
                    .thenReturn(transactionalOperator);

            Mockito.when(transactionalOperator.transactional(Mockito.any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StepVerifier.create(onboardingRequestEvaluatorService.updateInitiativeBudget(evaluationDTO, initiativeConfig))
                    .expectNext(evaluationDTO)
                    .verifyComplete();
        }
    }

    @Test
    void updateInitiativeBudget_notDeallocateForOnboarding_OK(){
        // Given
        EvaluationCompletedDTO completed = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId(initiativeConfig.getInitiativeId())
                .verifyIsee(true)
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .build();


        // When
        EvaluationDTO result = onboardingRequestEvaluatorService.updateInitiativeBudget(completed, initiativeConfig).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(completed, result);

        Mockito.verify(initiativeCountersPreallocationsRepository, Mockito.never()).deleteByIdReturningResult(Mockito.any());
        Mockito.verify(initiativeCountersRepository, Mockito.never()).deallocatedPartialBudget(Mockito.any(), Mockito.anyLong());
    }

    @Test
    void updateInitiativeBudget_notEvaluatedComplete(){
        RankingRequestDTO evaluation = new RankingRequestDTO();

        EvaluationDTO result = onboardingRequestEvaluatorService.updateInitiativeBudget(evaluation, initiativeConfig).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(evaluation, result);
    }




}
