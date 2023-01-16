package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

//class IseeConsultationSoapClientImplTest {
class IseeConsultationSoapClientImplTest extends BaseIntegrationTest {

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;


    @Test
    void callService() {
//        IseeConsultationSoapClient iseeConsultationSoapClient = new IseeConsultationSoapClientImpl("001","OperationBatchIDPay","https://api.collaudo.inps.it/pdnd/soap/ConsultazioneISEE/v1", certInps);

        ConsultazioneIndicatoreResponse result = iseeConsultationSoapClient.callService("RSSMRA70A01H501S").block();
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}
