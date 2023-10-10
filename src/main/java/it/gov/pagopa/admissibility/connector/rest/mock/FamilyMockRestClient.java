package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import reactor.core.publisher.Mono;

public interface FamilyMockRestClient {//TODO removeme once integrated real system
    Mono<Family> retrieveFamily(String userId);
}
