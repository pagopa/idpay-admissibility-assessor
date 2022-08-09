package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRequestEvaluatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AdmissibilityEvaluatorMediatorServiceImplTest {

    @Test
    void mediatorTest() {

        // Given
        OnboardingCheckService onboardingCheckService = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverService = Mockito.mock(AuthoritiesDataRetrieverService.class);
        OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock = Mockito.mock(OnboardingRequestEvaluatorService.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

        AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingCheckService, authoritiesDataRetrieverService, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper);

        OnboardingDTO onboarding1 = new OnboardingDTO();
        OnboardingDTO onboarding2 = new OnboardingDTO();
        Flux<OnboardingDTO> onboardingFlux = Flux.just(onboarding1,onboarding2);

        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckService.check(Mockito.same(onboarding2), Mockito.any())).thenReturn("Rejected");

        Mockito.when(authoritiesDataRetrieverService.retrieve(Mockito.same(onboarding1), Mockito.any())).thenAnswer(i-> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.same(onboarding1), Mockito.any())).thenAnswer(i-> Mono.just(i.getArgument(0)));

        // When
        List<EvaluationDTO> result = admissibilityEvaluatorMediatorService.execute(onboardingFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }
}
