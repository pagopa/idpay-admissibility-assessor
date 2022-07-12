package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;

/**
 * Given an {@link OnboardingDTO} it will fetch the authorities' data used by the {@link OnboardingDTO#getInitiativeId()}
 * returning true if it was able to retrieve the data and false if any authority overflows the daily limit
 */
public interface AuthoritiesDataRetrieverService {

    boolean retrieve(OnboardingDTO onboardingDTO);
}
