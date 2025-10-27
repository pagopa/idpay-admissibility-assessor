package it.gov.pagopa.admissibility.connector.rest.onboarding;

import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

public interface OnboardingRestClient {
    Mono<Pair<Boolean, String>> alreadyOnboardingStatus(String initiativeId, String userId);
}
