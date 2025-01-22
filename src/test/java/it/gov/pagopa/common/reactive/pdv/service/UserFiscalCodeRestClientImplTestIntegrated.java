package it.gov.pagopa.common.reactive.pdv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(
        properties = {
                "app.pdv.base-url=https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1",
        },
        locations = {
                "classpath:/secrets/appPdv.properties"
        })
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stub/mappings/pdv")
@SpringBootTest(classes = {UserFiscalCodeRestClientImpl.class, WebClientConfig.class, ObjectMapper.class})
class UserFiscalCodeRestClientImplTestIntegrated {
    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;

    @Value("${app.pdv.userIdOk:a85268f9-1d62-4123-8f86-8cf630b60998}")
    private String userIdOK;
    @Value("${app.pdv.userFiscalCodeExpected:A4p9Y4QUlTtutHT}")
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
