package it.gov.pagopa.admissibility.rest;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.rest.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SuppressWarnings("squid:S3577") // suppressing class name not match alert
@TestPropertySource(locations = {
        "classpath:/secrets/appPdv.properties"
})
@ContextConfiguration(inheritInitializers = false)
class UserFiscalCodeRestClientImplTestIntegrated extends BaseIntegrationTest {
    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;

    @Value("${app.pdv.userIdOk:02105b50-9a81-4cd2-8e17-6573ebb09196}")
    private String userIdOK;
    @Value("${app.pdv.userFiscalCodeExpected:125}")
    private String fiscalCodeOKExpected;
    @Value("${app.pdv.userIdNotFound:02105b50-9a81-4cd2-8e17-6573ebb09195}")
    private String userIdNotFound;

    @Test
    void retrieveUserInfoOk() {
        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo(userIdOK).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fiscalCodeOKExpected,result.getPii());

    }

    @Test
    void retrieveUserInfoNotFound() {
        try{
            userFiscalCodeRestClient.retrieveUserInfo(userIdNotFound).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.NotFound.class,e.getClass());
        }
    }
}
