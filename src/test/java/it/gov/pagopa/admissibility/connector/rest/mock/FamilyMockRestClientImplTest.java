package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;


@ContextConfiguration(
        classes = {
                FamilyMockRestClientImpl.class,
                WebClientConfig.class
        })
@TestPropertySource(
        properties = {
                WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX + "app.idpay-mock.base-url=pdndMock"
        }
)
class FamilyMockRestClientImplTest extends BaseWireMockTest {

    @Autowired
    private FamilyMockRestClientImpl familyMockRestClient;

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
