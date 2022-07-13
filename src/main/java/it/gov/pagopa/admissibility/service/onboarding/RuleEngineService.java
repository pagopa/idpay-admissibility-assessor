package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;

/**
 * This component will take {@link OnboardingDTO} and will check if it can join the initiative
 * */
public interface RuleEngineService {
    EvaluationDTO applyRules(OnboardingDTO onboardingDTO);
}
