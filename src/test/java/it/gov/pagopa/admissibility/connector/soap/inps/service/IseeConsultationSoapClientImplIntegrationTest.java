package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.soap.inps.utils=DEBUG",
})
@DirtiesContext
class IseeConsultationSoapClientImplIntegrationTest extends BaseIntegrationTest {

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_OK", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.OK, result.getEsito());
        Assertions.assertEquals(0, result.getIdRichiesta());

        TypeEsitoConsultazioneIndicatore indicatore = InpsDataRetrieverServiceImpl.readResultFromXmlString(new String(result.getXmlEsitoIndicatore(), StandardCharsets.UTF_8));
        Assertions.assertNotNull(indicatore);
        Assertions.assertEquals(BigDecimal.valueOf(10_000), indicatore.getISEE());
    }

    @Test
    void getIseeInvalidRequest(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_INVALID_REQUEST", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.RICHIESTA_INVALIDA, result.getEsito());
    }

    @Test
    void getIseeNotFound(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_NOT_FOUND", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.DATI_NON_TROVATI, result.getEsito());
    }

    @Test
    void getIseeRetry(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_INPS_RETRY", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNull(result);
    }
}
