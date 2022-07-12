package it.gov.pagopa.service.onboarding.check;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Check if the onboarding is valid and return a not null String describing the failing check,
 * otherwise it will return null
 * */
public interface OnboardingCheck extends Function<OnboardingDTO, String> {
}
