package it.gov.pagopa.admissibility.connector.pdnd;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import reactor.core.publisher.Mono;

public interface PdndRestClient {
    Mono<ClientCredentialsResponseDTO> createToken(String clientId, String clientAssertion);
}