package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import reactor.core.publisher.Mono;

//TODO removeme once integrated real system
public interface FamilyMockRestClient {
    Mono<Family> retrieveFamily(String userId);
}
