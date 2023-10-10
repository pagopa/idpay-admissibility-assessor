package it.gov.pagopa.common.reactive.pdv.service;

import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeRestClientImpl=WARN",

        "app.pdv.base-url=http://localhost:${wiremock.server.port}/pdv",
        "app.pdv.retry.delay-millis=100",
        "app.pdv.retry.max-attempts=1",
        "app.pdv.headers.x-api-key=x_api_key"
})
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stub/mappings/pdv")
@SpringBootTest(classes = {UserFiscalCodeRestClientImpl.class, WebClientConfig.class})
class UserFiscalCodeRestClientImplTest {
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
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.Forbidden.class,e.getClass());
        }
    }
}
