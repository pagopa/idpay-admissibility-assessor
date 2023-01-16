package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.soap.inps.utils=DEBUG",
})
class IseeConsultationSoapClientImplTest extends BaseIntegrationTest {

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponse result = iseeConsultationSoapClient.callService("RSSMRA70A01H501S").block();
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}
