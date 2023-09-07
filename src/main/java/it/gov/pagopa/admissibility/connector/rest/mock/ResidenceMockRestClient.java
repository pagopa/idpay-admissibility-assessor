package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import reactor.core.publisher.Mono;

public interface ResidenceMockRestClient {
    Mono<Residence> retrieveResidence(String userId);
}
