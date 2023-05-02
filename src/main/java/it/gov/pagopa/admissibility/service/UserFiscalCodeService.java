package it.gov.pagopa.admissibility.service;

import reactor.core.publisher.Mono;

public interface UserFiscalCodeService {
    Mono<String> getUserFiscalCode(String userId);
}