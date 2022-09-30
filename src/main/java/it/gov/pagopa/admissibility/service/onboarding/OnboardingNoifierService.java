package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;

public interface OnboardingNoifierService {
    boolean notify(EvaluationDTO evaluationDTO);
}
