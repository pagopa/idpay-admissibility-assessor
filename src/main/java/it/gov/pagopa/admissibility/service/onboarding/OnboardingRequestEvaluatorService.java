package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import reactor.core.publisher.Mono;

/** It will evaluate {@link OnboardingDTO} request and reserve budget */
public interface OnboardingRequestEvaluatorService {
    Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig);
}
