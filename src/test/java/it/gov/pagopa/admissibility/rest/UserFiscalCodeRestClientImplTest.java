package it.gov.pagopa.admissibility.rest;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.rest.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.rest.UserFiscalCodeRestClientImpl=WARN",
})
class UserFiscalCodeRestClientImplTest extends BaseIntegrationTest {
    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;


    @Test
    void retrieveUserInfoOk() {
        String userId = "USERID_OK_1";

        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo(userId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("fiscalCode",result.getPii());
    }

    @Test
    void retrieveUserInfoNotFound() {
        String userId = "USERID_NOTFOUND_1";

        try{
            userFiscalCodeRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.NotFound.class,e.getClass());
        }
    }

    @Test
    void retrieveUserInfoInternalServerError() {
        String userId = "USERID_INTERNALSERVERERROR_1";

        try{
            userFiscalCodeRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.InternalServerError.class,e.getClass());
        }
    }

    @Test
    void retrieveUserInfoBadRequest() {
        String userId = "USERID_BADREQUEST_1";

        try{
            userFiscalCodeRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.BadRequest.class,e.getClass());
        }
    }

    @Test
    void retrieveUserInfoTooManyRequest() {
        String userId = "USERID_TOOMANYREQUEST_1";

        try{
            userFiscalCodeRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }

    @Test
    void retrieveUserInfoHttpForbidden() {
        String userId = "USERID_FORBIDDEN_1";

        try{
            userFiscalCodeRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            e.printStackTrace();
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.Forbidden.class,e.getClass());
        }
    }
}
