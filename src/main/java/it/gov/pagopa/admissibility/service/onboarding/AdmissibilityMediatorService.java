package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import reactor.core.publisher.Flux;

public interface AdmissibilityMediatorService {

    Flux<EvaluationDTO> execute(Flux<OnboardingDTO>  onboardingDTOFlux);
}
