package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;

/**
 * This component will take {@link OnboardingDTO} and will check if it can join the initiative
 * */
public interface RuleEngineService {
    EvaluationDTO applyRules(OnboardingDTO onboardingDTO);
}
