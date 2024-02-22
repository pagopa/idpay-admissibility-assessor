package it.gov.pagopa.admissibility.connector.rest.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.Exceptions;

@ExtendWith(SpringExtension.class)
@AutoConfigureWireMock(stubs ="classpath:/stub/mappings/mock" ,port =800)
@ContextConfiguration(
        classes = {
                FamilyMockRestClientImpl.class,
                WebClientConfig.class,
                WireMockServer.class
        })
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {"spring.application.name=idpay-admissibility-assessor",
                "app.idpay-mock.base-url=http://localhost:${wiremock.server.port}/pdndMock",
                //Region familyMockRestClient
                "app.idpay-mock.retry.delay-millis=1",
                "app.idpay-mock.retry.max-attempts=5",
                //endRegion

                //Region webClientConfig
               "app.web-client.connect.timeout.millis=10000",
                "app.web-client.response.timeout=60000",
                "app.web-client.read.handler.timeout=60000",
                "app.web-client.write.handler.timeout=60000"
                //endRegion
                })
@Slf4j
class FamilyMockRestClientImplTest {
    @Autowired
    private FamilyMockRestClientImpl familyMockRestClient;
    @Autowired
    private WireMockServer wireMockServer;

    @Test
    void givenUserIdWhenCallFamilyMockRestClientThenFamily() {

        Family family = familyMockRestClient.retrieveFamily("userId_1").block();
        Assertions.assertEquals(3,family.getMemberIds().size());
    }

    @Test
    void givenUserIdWhenCallFamilyMockRestClientThenTooManyRequestException(){
        try{
            familyMockRestClient.retrieveFamily("USERID_TOOMANYREQUEST_1").block();
            Assertions.fail();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }
}
