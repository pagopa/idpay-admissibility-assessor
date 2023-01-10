package it.gov.pagopa.admissibility.rest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import reactor.core.publisher.Mono;

public interface PdndCreateTokenRestClient {
    Mono<ClientCredentialsResponseDTO> createToken(String pdndToken);
}