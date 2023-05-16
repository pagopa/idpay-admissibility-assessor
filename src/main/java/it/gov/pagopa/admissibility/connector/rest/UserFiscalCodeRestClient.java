package it.gov.pagopa.admissibility.connector.rest;

import it.gov.pagopa.admissibility.dto.rest.UserInfoPDV;
import reactor.core.publisher.Mono;

public interface UserFiscalCodeRestClient {
    Mono<UserInfoPDV> retrieveUserInfo(String userId);
}
