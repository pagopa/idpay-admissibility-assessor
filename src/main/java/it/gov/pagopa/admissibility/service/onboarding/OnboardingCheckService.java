package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.service.onboarding.check.OnboardingCheck;

import java.util.Map;

/**
 * This component will take a {@link OnboardingDTO} and will test it against all the {@link OnboardingCheck} configured returning a rejection reason if any, otherwise null
 * */
public interface OnboardingCheckService {
    OnboardingRejectionReason check(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext);
}
