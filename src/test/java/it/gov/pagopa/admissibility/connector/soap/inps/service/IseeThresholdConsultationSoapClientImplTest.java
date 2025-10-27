
package it.gov.pagopa.admissibility.connector.soap.inps.service;

import com.sun.xml.ws.client.ClientTransportException;
import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.*;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class IseeThresholdConsultationSoapClientImplTest {

    @Mock
    private ISvcConsultazione svcConsultazione;

    @Mock
    private InpsThresholdClientConfig inpsThresholdClientConfig;

    private IseeThresholdConsultationSoapClientImpl client;

    @BeforeEach
    void setUp() {
        Mockito.when(inpsThresholdClientConfig.getPortSvcConsultazione()).thenReturn(svcConsultazione);
        client = new IseeThresholdConsultationSoapClientImpl(inpsThresholdClientConfig);
    }

    @Test
    void shouldReturnResultWhenEsitoIsOk(){
        var responseType = new ConsultazioneSogliaIndicatoreResponseType();
        responseType.setEsito(EsitoEnum.OK);
        responseType.setIdRichiesta(1);

        var response = new ConsultazioneSogliaIndicatoreResponse();
        response.setConsultazioneSogliaIndicatoreResult(responseType);

        CompletableFuture<ConsultazioneSogliaIndicatoreResponse> future = CompletableFuture.completedFuture(response);
        Mockito.when(svcConsultazione.consultazioneSogliaIndicatoreAsync(any(), any())).thenAnswer(invocation -> {
            AsyncHandler<ConsultazioneSogliaIndicatoreResponse> handler = invocation.getArgument(1);
            handler.handleResponse(getResponse(response));
            return future;
        });

        StepVerifier.create(client.verifyThresholdIsee("ABCDEF12G34H567I", "SOGLIA"))
                .expectNextMatches(result -> result.getEsito() == EsitoEnum.OK)
                .verifyComplete();
    }

    @NotNull
    private static Response<ConsultazioneSogliaIndicatoreResponse> getResponse(ConsultazioneSogliaIndicatoreResponse response) {
        return new Response<>() {

            @Override
            public Map<String, Object> getContext() {
                return new HashMap<>();
            }

            @Override
            public ConsultazioneSogliaIndicatoreResponse get() {
                return response;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public ConsultazioneSogliaIndicatoreResponse get(long timeout, TimeUnit unit) {
                return response;
            }
        };
    }

    @Test
    void shouldReturnEmptyWhenRetryableEsito(){
        var responseType = new ConsultazioneSogliaIndicatoreResponseType();
        responseType.setEsito(EsitoEnum.DATABASE_OFFLINE);

        var response = new ConsultazioneSogliaIndicatoreResponse();
        response.setConsultazioneSogliaIndicatoreResult(responseType);

        CompletableFuture<ConsultazioneSogliaIndicatoreResponse> future = CompletableFuture.completedFuture(response);
        Mockito.when(svcConsultazione.consultazioneSogliaIndicatoreAsync(any(), any())).thenAnswer(invocation -> {
            AsyncHandler<ConsultazioneSogliaIndicatoreResponse> handler = invocation.getArgument(1);
            handler.handleResponse(getResponse(response));
            return future;
        });

        StepVerifier.create(client.verifyThresholdIsee("ABCDEF12G34H567I", "SOGLIA"))
                .expectComplete()
                .verify();
    }


    @Test
    void shouldThrowInpsDailyRequestLimitExceptionOnTooManyRequests() {
        ExecutionException executionException = new ExecutionException(
                new ClientTransportException(new RuntimeException("Too Many Requests"))
        );
        Mockito.when(svcConsultazione.consultazioneSogliaIndicatoreAsync(any(), any()))
                .thenAnswer(invocation -> {
                    throw executionException;
                });

        StepVerifier.create(client.verifyThresholdIsee("ABCDEF12G34H567I", "SOGLIA"))
                .expectError(InpsDailyRequestLimitException.class)
                .verify();
    }




    @Test
    void shouldThrowIllegalStateExceptionOnGenericError() {
        ExecutionException executionException = new ExecutionException(new RuntimeException("Generic error"));

        Mockito.when(svcConsultazione.consultazioneSogliaIndicatoreAsync(any(), any()))
                .thenAnswer(invocation -> {
                    throw executionException;
                });


        StepVerifier.create(client.verifyThresholdIsee("ABCDEF12G34H567I", "SOGLIA"))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
