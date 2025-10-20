package it.gov.pagopa.admissibility.connector.rest.onboarding;

import reactor.core.publisher.Mono;

public interface OnboardingRestClient {
    ///idpay/onboarding
    ///{initiativeId}/{userId}/status
    Mono<Boolean> alreadyOnboardingStatus(String initiativeId, String userId);
}
