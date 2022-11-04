package it.gov.pagopa.admissibility.service.pdnd;

import reactor.core.publisher.Mono;

public interface UserFiscalCodeService {
    Mono<String> getUserFiscalCode(String userId);
}