package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.service.onboarding.check.OnboardingCheck;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;

/**
 * This component will take a {@link OnboardingDTO} and will test it against all the {@link OnboardingCheck} configured returning a rejection reason if any, otherwise null
 * */
public interface OnboardingCheckService {
    String check(OnboardingDTO onboardingDTO);
}
