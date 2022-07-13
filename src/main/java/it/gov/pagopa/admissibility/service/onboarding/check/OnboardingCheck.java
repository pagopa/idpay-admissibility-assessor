package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;

import java.util.function.Function;

/**
 * Check if the onboarding is valid and return a not null String describing the failing check,
 * otherwise it will return null
 * */
public interface OnboardingCheck extends Function<OnboardingDTO, String> {
}
