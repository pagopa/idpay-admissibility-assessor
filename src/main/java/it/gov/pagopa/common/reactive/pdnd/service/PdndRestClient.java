package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.common.pdnd.generated.dto.ClientCredentialsResponseDTO;
import reactor.core.publisher.Mono;

public interface PdndRestClient {
    Mono<ClientCredentialsResponseDTO> createToken(String clientId, String clientAssertion);
}