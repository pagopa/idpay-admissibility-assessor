package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.rest.mock.ResidenceMockRestClientImpl=WARN",
        "app.idpay-mock.retry.max-attempts=1",
        "app.idpay-mock.retry.delay-millis=100"
})
class ResidenceMockRestClientImplTest extends BaseIntegrationTest {

    public static final String MILANO = "Milano";

    @Autowired
    private ResidenceMockRestClient residenceMockRestClient;
    @Test
    void retrieveResidence() {

        String userId = "userId_1";

        Residence result = residenceMockRestClient.retrieveResidence(userId).block();

        Assertions.assertNotNull(result);

        Residence expectedResidence = Residence.builder()
                .city(MILANO)
                .cityCouncil(MILANO)
                .province(MILANO)
                .region("Lombardia")
                .postalCode("20124")
                .nation("Italia")
                .build();

        Assertions.assertEquals(expectedResidence, result);

    }

    @Test
    void retrieveResidenceTooManyRequest() {
        String userId = "USERID_TOOMANYREQUEST_1";

        try{
            residenceMockRestClient.retrieveResidence(userId).block();
            Assertions.fail();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }
}