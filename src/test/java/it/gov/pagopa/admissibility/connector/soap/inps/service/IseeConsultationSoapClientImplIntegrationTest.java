package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.soap.service.SoapLoggingHandler=DEBUG"
})
@DirtiesContext
public class IseeConsultationSoapClientImplIntegrationTest extends BaseIntegrationTest {

    public static final String FISCAL_CODE_OK = "CF_OK";
    public static final String FISCAL_CODE_NOTFOUND = "CF_NOT_FOUND";
    public static final String FISCAL_CODE_INVALIDREQUEST = "CF_INVALID_REQUEST";
    public static final String FISCAL_CODE_RETRY = "CF_INPS_RETRY";
    public static final String FISCAL_CODE_UNEXPECTED_RESULT_CODE = "CF_INPS_UNEXPECTED_RESULT_CODE";
    public static final String FISCAL_CODE_FAULT_MESSAGE = "CF_INPS_FAULT_MESSAGE";
    public static final String FISCAL_CODE_TOOMANYREQUESTS = "CF_INPS_TOO_MANY_REQUESTS";

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_OK, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.OK, result.getEsito());
        Assertions.assertEquals(0, result.getIdRichiesta());

        TypeEsitoConsultazioneIndicatore indicatore = InpsDataRetrieverServiceImpl.readResultFromXmlString(new String(result.getXmlEsitoIndicatore(), StandardCharsets.UTF_8));
        Assertions.assertNotNull(indicatore);
        Assertions.assertEquals(BigDecimal.valueOf(10_000), indicatore.getISEE());
    }

    @Test
    void getIseeNotFound(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_NOTFOUND, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.DATI_NON_TROVATI, result.getEsito());
    }

    @Test
    void getIseeInvalidRequest(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_INVALIDREQUEST, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(EsitoEnum.RICHIESTA_INVALIDA, result.getEsito());
    }

    @Test
    void getIseeRetry(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_RETRY, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNull(result);
    }

    @Test
    void getIseeUnexpectedResultCode(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_UNEXPECTED_RESULT_CODE, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getXmlEsitoIndicatore());
    }

    @Test
    void getIseeTooManyRequests(){
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_TOOMANYREQUESTS, IseeTypologyEnum.ORDINARIO);
        Assertions.assertThrows(InpsDailyRequestLimitException.class, mono::block);
    }

    @Test
    void getIseeFaultMessage(){
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee(FISCAL_CODE_FAULT_MESSAGE, IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getXmlEsitoIndicatore());
    }
}
