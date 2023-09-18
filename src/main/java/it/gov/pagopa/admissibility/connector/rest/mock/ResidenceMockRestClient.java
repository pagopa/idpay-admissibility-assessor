package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import reactor.core.publisher.Mono;

public interface ResidenceMockRestClient {//TODO removeme once integrated real system
    Mono<Residence> retrieveResidence(String userId);
}
