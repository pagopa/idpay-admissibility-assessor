package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.rest.mock.FamilyMockRestClientImpl=WARN",
        "app.idpay-mock.retry.max-attempts=1",
        "app.idpay-mock.retry.delay-millis=100"
})
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"})
class FamilyMockRestClientImplTestDeprecated extends BaseIntegrationTest {


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