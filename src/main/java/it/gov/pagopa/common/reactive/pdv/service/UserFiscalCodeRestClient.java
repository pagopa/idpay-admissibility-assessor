package it.gov.pagopa.common.reactive.pdv.service;

import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import reactor.core.publisher.Mono;

public interface UserFiscalCodeRestClient {
    Mono<UserInfoPDV> retrieveUserInfo(String userId);

    Mono<UserInfoPDV> retrieveUserId(String fiscalCode);
}
