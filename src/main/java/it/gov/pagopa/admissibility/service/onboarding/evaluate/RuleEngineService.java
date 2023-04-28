package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;

/**
 * This component will take {@link OnboardingDTO} and will check if it can join the initiative
 * */
public interface RuleEngineService {
    EvaluationDTO applyRules(OnboardingDTO onboardingDTO, InitiativeConfig initiative);
}
