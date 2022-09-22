package it.gov.pagopa.admissibility.exception;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.controller.AdmissibilityController;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Flux;

class ErrorManagerTest extends BaseIntegrationTest {

    @SpyBean
    AdmissibilityController admissibilityController;

    @Test
    void handleException() {
        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionNoBody"))
                .thenThrow(ClientExceptionNoBody.class);

        Flux<InitiativeStatusDTO> ExceptionNoBodyFlux = Flux.defer(() -> admissibilityController.getInitiativeStatus("ClientExceptionNoBody"))
                .onErrorResume(e -> {
                    Assertions.assertTrue(e instanceof ClientExceptionNoBody);
                    return Flux.empty();
                });
        Assertions.assertEquals(0, ExceptionNoBodyFlux.count().block());

        Mockito.when(admissibilityController.getInitiativeStatus("ClientException"))
                .thenThrow(ClientException.class);
        Flux<InitiativeStatusDTO> ClientExceptionFlux = Flux.defer(() -> admissibilityController.getInitiativeStatus("ClientException"))
                .onErrorResume(e -> {
                    Assertions.assertTrue(e instanceof ClientException);
                    return Flux.empty();
                });
        Assertions.assertEquals(0, ClientExceptionFlux.count().block());

    }
}