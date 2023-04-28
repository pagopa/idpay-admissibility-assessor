package it.gov.pagopa.admissibility.service.onboarding.notifier;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;

public interface OnboardingNotifierService {
    boolean notify(EvaluationDTO evaluationDTO);
}
