package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.rest.mock.FamilyMockRestClientImpl=WARN",
})
class FamilyMockRestClientImplTest extends BaseIntegrationTest {


    @Autowired
    private FamilyMockRestClient familyMockRestClient;


    @Test
    void retrieveFamilyTooManyRequest() {
        String userId = "USERID_TOOMANYREQUEST_1";

        try{
            familyMockRestClient.retrieveFamily(userId).block();
            Assertions.fail();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }
}