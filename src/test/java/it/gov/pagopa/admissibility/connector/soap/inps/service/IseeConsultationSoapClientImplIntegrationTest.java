package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.common.soap.service.SoapLoggingHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@ContextConfiguration(
        classes = {
                IseeConsultationSoapClientImpl.class,
                InpsClientConfig.class,
                SoapLoggingHandler.class,
                WebClientConfig.class
        })
public class IseeConsultationSoapClientImplIntegrationTest extends BaseWireMockTest {

    public static final String FISCAL_CODE_OK = "CF_OK";
    public static final String FISCAL_CODE_NOTFOUND = "CF_NOT_FOUND";
    public static final String FISCAL_CODE_INVALIDREQUEST = "CF_INVALID_REQUEST";
    public static final String FISCAL_CODE_RETRY = "CF_INPS_RETRY";
    public static final String FISCAL_CODE_UNEXPECTED_RESULT_CODE = "CF_INPS_UNEXPECTED_RESULT_CODE";
    public static final String FISCAL_CODE_FAULT_MESSAGE = "CF_INPS_FAULT_MESSAGE";
    public static final String FISCAL_CODE_TOOMANYREQUESTS = "CF_INPS_TOO_MANY_REQUESTS";

    @Autowired
    private IseeConsultationSoapClientImpl iseeConsultationSoapClient;

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
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_UNEXPECTED_RESULT_CODE, IseeTypologyEnum.ORDINARIO);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, mono::block);
        Assertions.assertEquals("[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", exception.getMessage());
    }

    @Test
    void getIseeTooManyRequests(){
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_TOOMANYREQUESTS, IseeTypologyEnum.ORDINARIO);
        Assertions.assertThrows(InpsDailyRequestLimitException.class, mono::block);
    }

    @Test
    void getIseeFaultMessage(){
        Mono<ConsultazioneIndicatoreResponseType> mono = iseeConsultationSoapClient.getIsee(FISCAL_CODE_FAULT_MESSAGE, IseeTypologyEnum.ORDINARIO);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, mono::block);
        Assertions.assertEquals("[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", exception.getMessage());

    }
}
