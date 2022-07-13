package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Check if the onboarding is valid and return a not null String describing the failing check,
 * otherwise it will return null
 * */
public interface OnboardingCheck extends BiFunction<OnboardingDTO, Map<String, Object>, String> {
}
