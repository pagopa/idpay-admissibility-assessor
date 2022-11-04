package it.gov.pagopa.admissibility.service.pdnd;

import reactor.core.publisher.Mono;

public interface CreateTokenService { //TODO
    Mono<String> getToken(String pdndToken);
}