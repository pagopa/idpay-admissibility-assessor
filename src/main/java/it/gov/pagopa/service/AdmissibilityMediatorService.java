package it.gov.pagopa.service;

import it.gov.pagopa.dto.EvaluationDTO;
import it.gov.pagopa.dto.OnboardingDTO;
import reactor.core.publisher.Flux;

public interface AdmissibilityMediatorService {

    Flux<EvaluationDTO> execute(Flux<OnboardingDTO>  onboardingDTOFlux);
}
