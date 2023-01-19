package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.soap.inps.utils=DEBUG",
        "app.inps.iseeConsultation.base-url=https://api-io.dev.cstar.pagopa.it/mock-ex-serv-inps" //Temporary calling Mocked APIM
})
@ContextConfiguration(inheritInitializers = false)
class IseeConsultationSoapClientImplTestIntegrated extends BaseIntegrationTest {

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_OK").block();
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}
