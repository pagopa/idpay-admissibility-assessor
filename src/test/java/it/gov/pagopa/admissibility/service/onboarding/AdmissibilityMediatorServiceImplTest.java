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

@ExtendWith(MockitoExtension.class)
class AdmissibilityMediatorServiceImplTest {

    @Test
    void mediatorTest() { //TODO check this test, it print ERROR even if not shown

        // Given
        OnboardingCheckService onboardingCheckService = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverService = Mockito.mock(AuthoritiesDataRetrieverServiceImpl.class);
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = Mockito.mock(Onboarding2EvaluationMapper.class);
        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        AdmissibilityMediatorService admissibilityMediatorService = new AdmissibilityMediatorServiceImpl(onboardingCheckService, authoritiesDataRetrieverService, ruleEngineService, onboarding2EvaluationMapper);

        OnboardingDTO onboarding1 = Mockito.mock(OnboardingDTO.class);
        OnboardingDTO onboarding2 = Mockito.mock(OnboardingDTO.class);
        Flux<OnboardingDTO> onboardingFlux = Flux.just(onboarding1,onboarding2);

        EvaluationDTO evaluationDTO = Mockito.mock(EvaluationDTO.class);

        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding2), Mockito.any())).thenReturn("Rejected");

        Mockito.when(authoritiesDataRetrieverService.retrieve(onboarding1, initiativeConfig)).thenReturn(true);
        Mockito.when(ruleEngineService.applyRules(onboarding1)).thenReturn(evaluationDTO);
        Mockito.when(onboarding2EvaluationMapper.apply(onboarding2, Collections.singletonList("Rejected"))).thenReturn(evaluationDTO);

        // When
        Flux<EvaluationDTO> result = admissibilityMediatorService.execute(onboardingFlux);

        // Then
        result.count().subscribe(i -> Assertions.assertEquals(2L, i));
    }
}
