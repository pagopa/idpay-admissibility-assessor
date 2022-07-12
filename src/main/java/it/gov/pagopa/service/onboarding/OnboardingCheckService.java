package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;

/**
 * This component will take a {@link OnboardingDTO} and will test it against all the {@link it.gov.pagopa.service.onboarding.check.OnboardingCheck} configured returning a rejection reason if any, otherwise null
 * */
public interface OnboardingCheckService {
    String check(OnboardingDTO onboardingDTO);
}
