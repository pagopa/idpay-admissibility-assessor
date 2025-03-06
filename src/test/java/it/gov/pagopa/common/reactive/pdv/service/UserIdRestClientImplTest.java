package it.gov.pagopa.common.reactive.pdv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeRestClientImpl=WARN",

        "app.pdv.base-url=http://localhost:${wiremock.server.port}/pdv",
        "app.pdv.retry.delay-millis=100",
        "app.pdv.retry.max-attempts=1",
        "app.pdv.headers.x-api-key=x_api_key"
})
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stub/mappings/pdv")
@SpringBootTest(classes = {UserFiscalCodeRestClientImpl.class, WebClientConfig.class, ObjectMapper.class})
class UserIdRestClientImplTest {



    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;


    @Test
    void retrieveUserInfoOk() {
        String userId = "CF_OK";

        UserIdPDV result = userFiscalCodeRestClient.retrieveUserId(userId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("USERID_OK",result.getToken());
    }

    @Test
    void retrieveUserInfoNotFound() {
        String userId = "USER_NOT_FOUND_1";

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

}
