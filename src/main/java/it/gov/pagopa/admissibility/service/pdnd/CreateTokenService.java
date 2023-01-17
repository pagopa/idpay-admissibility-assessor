package it.gov.pagopa.admissibility.service.pdnd;

import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import reactor.core.publisher.Mono;

public interface CreateTokenService {
    Mono<String> getToken(ApiKeysPDND pdndToken);
}