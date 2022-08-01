package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AdmissibilityMediatorServiceImplTest {

    @Test
    void mediatorTest() { //TODO check this test, it prints ERROR even if not shown

        // Given
        OnboardingCheckService onboardingCheckService = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverService = Mockito.mock(AuthoritiesDataRetrieverService.class);
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = Mockito.mock(Onboarding2EvaluationMapper.class);

        AdmissibilityMediatorService admissibilityMediatorService = new AdmissibilityMediatorServiceImpl(onboardingCheckService, authoritiesDataRetrieverService, ruleEngineService, onboarding2EvaluationMapper);

        OnboardingDTO onboarding1 = new OnboardingDTO();
        OnboardingDTO onboarding2 = new OnboardingDTO();
        Flux<OnboardingDTO> onboardingFlux = Flux.just(onboarding1,onboarding2);

        EvaluationDTO evaluationDTO = new EvaluationDTO();

        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding2), Mockito.any())).thenReturn("Rejected");

        Mockito.when(authoritiesDataRetrieverService.retrieve(Mockito.same(onboarding1), Mockito.any())).thenReturn(true);
        Mockito.when(ruleEngineService.applyRules(Mockito.same(onboarding1))).thenReturn(evaluationDTO);
        Mockito.when(onboarding2EvaluationMapper.apply(Mockito.same(onboarding2), Mockito.eq(Collections.singletonList("Rejected")))).thenReturn(evaluationDTO);

        // When
        List<EvaluationDTO> result = admissibilityMediatorService.execute(onboardingFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }
}
