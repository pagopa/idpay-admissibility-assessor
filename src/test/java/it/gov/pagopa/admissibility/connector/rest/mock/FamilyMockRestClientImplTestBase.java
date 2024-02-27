package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.Exceptions;


@ContextConfiguration(
        classes = {
                FamilyMockRestClientImpl.class,
                WebClientConfig.class
        })
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
