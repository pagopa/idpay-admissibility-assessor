package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;

/**
 * Given an {@link OnboardingDTO} it will fetch the authorities' data used by the {@link OnboardingDTO#getInitiativeId()}
 * returning true if it was able to retrieve the data and false if any authority overflows the daily limit
 */
public interface AuthoritiesDataRetrieverService {

    boolean retrieve(OnboardingDTO onboardingDTO, InitiativeConfig initiativeConfig);
}
