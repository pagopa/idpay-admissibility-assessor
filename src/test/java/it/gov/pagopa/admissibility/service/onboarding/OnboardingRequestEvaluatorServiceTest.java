package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OnboardingRequestEvaluatorServiceTest {

    @Mock
    private RuleEngineService ruleEngineService;
    @Mock
    private InitiativeCountersRepository initiativeCountersRepository;

    @InjectMocks
    private OnboardingRequestEvaluatorServiceImpl onboardingRequestEvaluatorService;

    private final OnboardingDTO onboardingRequest = new OnboardingDTO();
    private final InitiativeConfig initiativeConfig = new InitiativeConfig();

    public OnboardingRequestEvaluatorServiceTest(){
        onboardingRequest.setInitiativeId("ID");

        initiativeConfig.setInitiativeBudget(BigDecimal.TEN);
        initiativeConfig.setBeneficiaryInitiativeBudget(BigDecimal.ONE);
    }

    @Test
    void testRejectedFromRuleEngine(){
        //given
        final List<OnboardingRejectionReason> ruleEngineMockedRejectionReason = List.of(OnboardingRejectionReason.builder().type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR).code("DUMMY_REJECTION_REASON").build());

        final EvaluationDTO mockedRuleEngineResult = EvaluationDTO.builder()
                .initiativeId(onboardingRequest.getInitiativeId())
                .status(OnboardingConstants.ONBOARDING_STATUS_KO)
                .onboardingRejectionReasons(new ArrayList<>(ruleEngineMockedRejectionReason))
                .build();

        Mockito.when(ruleEngineService.applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig))).thenAnswer(i-> mockedRuleEngineResult);

        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertSame(mockedRuleEngineResult, result);
        Assertions.assertEquals(OnboardingConstants.ONBOARDING_STATUS_KO, result.getStatus());
        Assertions.assertEquals(ruleEngineMockedRejectionReason, result.getOnboardingRejectionReasons());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));

        Mockito.verifyNoMoreInteractions(ruleEngineService);
        Mockito.verifyNoInteractions(initiativeCountersRepository);
    }

    private void configureSuccesfulRuleEngine(){
        Mockito.when(ruleEngineService.applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig))).thenAnswer(i-> EvaluationDTO.builder()
                .initiativeId(((OnboardingDTO)i.getArgument(0)).getInitiativeId())
                .status(OnboardingConstants.ONBOARDING_STATUS_OK)
                .onboardingRejectionReasons(new ArrayList<>())
                .build());
    }

    @Test
    void testRejectedNoBudget(){
        //given
        configureSuccesfulRuleEngine();
        Mockito.when(initiativeCountersRepository.reserveBudget(Mockito.same(onboardingRequest.getInitiativeId()), Mockito.same(initiativeConfig.getBeneficiaryInitiativeBudget())))
                .thenReturn(Mono.empty());

        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(OnboardingConstants.ONBOARDING_STATUS_KO, result.getStatus());
        Assertions.assertEquals(List.of(
                        OnboardingRejectionReason.builder()
                                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                                .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                                .build()
                )
                , result.getOnboardingRejectionReasons());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));
        Mockito.verify(initiativeCountersRepository).reserveBudget(Mockito.same(onboardingRequest.getInitiativeId()), Mockito.same(initiativeConfig.getBeneficiaryInitiativeBudget()));

        Mockito.verifyNoMoreInteractions(ruleEngineService, initiativeCountersRepository);
    }

    @Test
    void testSuccessful(){
        //give
        configureSuccesfulRuleEngine();
        Mockito.when(initiativeCountersRepository.reserveBudget(Mockito.same(onboardingRequest.getInitiativeId()), Mockito.same(initiativeConfig.getBeneficiaryInitiativeBudget())))
                .thenReturn(Mono.just(new InitiativeCounters()));

        //when
        final EvaluationDTO result = onboardingRequestEvaluatorService.evaluate(onboardingRequest, initiativeConfig).block();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(OnboardingConstants.ONBOARDING_STATUS_OK, result.getStatus());
        Assertions.assertEquals(Collections.emptyList(), result.getOnboardingRejectionReasons());

        Mockito.verify(ruleEngineService).applyRules(Mockito.same(onboardingRequest), Mockito.same(initiativeConfig));
        Mockito.verify(initiativeCountersRepository).reserveBudget(Mockito.same(onboardingRequest.getInitiativeId()), Mockito.same(initiativeConfig.getBeneficiaryInitiativeBudget()));

        Mockito.verifyNoMoreInteractions(ruleEngineService, initiativeCountersRepository);
    }
}