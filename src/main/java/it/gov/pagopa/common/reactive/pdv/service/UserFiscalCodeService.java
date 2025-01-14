package it.gov.pagopa.common.reactive.pdv.service;

import reactor.core.publisher.Mono;

public interface UserFiscalCodeService {
    Mono<String> getUserFiscalCode(String userId);

    Mono<String> getUserId(String fiscalCode);
}