package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;

import java.util.Map;

/**
 * Check if the onboarding is valid and return a not null String describing the failing check,
 * otherwise it will return null
 * */
public interface OnboardingCheck {
    OnboardingRejectionReason apply(OnboardingDTO onboardingDTO, InitiativeConfig initiativeConfig, Map<String, Object> stringObjectMap);
}
