package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import reactor.core.publisher.Flux;

public interface AdmissibilityEvaluatorMediatorService {

    Flux<EvaluationDTO> execute(Flux<OnboardingDTO>  onboardingDTOFlux);
}
