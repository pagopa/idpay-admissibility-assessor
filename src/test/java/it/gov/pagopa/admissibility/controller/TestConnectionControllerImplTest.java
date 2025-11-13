package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponse;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ISvcConsultazione;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class TestConnectionControllerImplTest {

    private ISvcConsultazione svcConsultazione;
    private InpsThresholdClientConfig clientConfig;
    private TestConnectionControllerImpl controller;

    @BeforeEach
    void setUp() {
        svcConsultazione = mock(ISvcConsultazione.class);
        clientConfig = mock(InpsThresholdClientConfig.class);
        when(clientConfig.getPortSvcConsultazione()).thenReturn(svcConsultazione);
        controller = new TestConnectionControllerImpl(clientConfig);
    }

    @Test
    void testGetThreshold() {
        // Arrange
        String threshold = "THRESHOLD_CODE";
        String userCode = "FISCAL_CODE";

        ConsultazioneSogliaIndicatoreResponse mockResponse = new ConsultazioneSogliaIndicatoreResponse();

        // Mock SoapUtils.soapInvoke2Mono
        try (MockedStatic<SoapUtils> mockedSoapUtils = mockStatic(SoapUtils.class)) {
            mockedSoapUtils.when(() -> SoapUtils.soapInvoke2Mono(any()))
                    .thenReturn(Mono.just(mockResponse));

            // Act
            Mono<ConsultazioneSogliaIndicatoreResponse> result = controller.getThreshold(threshold, userCode);

            // Assert
            StepVerifier.create(result)
                    .expectNext(mockResponse)
                    .verifyComplete();
        }
    }
}