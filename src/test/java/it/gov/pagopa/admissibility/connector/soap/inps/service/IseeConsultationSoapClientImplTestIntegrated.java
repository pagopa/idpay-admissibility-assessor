package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
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
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_OK", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}
