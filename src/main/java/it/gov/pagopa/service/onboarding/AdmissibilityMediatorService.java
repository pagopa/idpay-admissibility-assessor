package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import reactor.core.publisher.Flux;

public interface AdmissibilityMediatorService {

    Flux<EvaluationDTO> execute(Flux<OnboardingDTO>  onboardingDTOFlux);
}
