package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.soap.inps.utils=DEBUG",
})
@DirtiesContext
class IseeConsultationSoapClientImplTest extends BaseIntegrationTest {

    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    @Test
    void callService() {
        ConsultazioneIndicatoreResponseType result = iseeConsultationSoapClient.getIsee("CF_OK", IseeTypologyEnum.ORDINARIO).block();
        Assertions.assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void getIseeInvalidRequest(){
        Mono<ConsultazioneIndicatoreResponseType> result = iseeConsultationSoapClient.getIsee("CF_INVALID_REQUEST", IseeTypologyEnum.ORDINARIO);
        Assertions.assertEquals(Boolean.FALSE, result.hasElement().block());
    }
}
