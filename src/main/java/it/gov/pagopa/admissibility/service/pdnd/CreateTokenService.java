package it.gov.pagopa.admissibility.service.pdnd;

import reactor.core.publisher.Mono;

public interface CreateTokenService {
    Mono<String> getToken(String pdndToken);
}