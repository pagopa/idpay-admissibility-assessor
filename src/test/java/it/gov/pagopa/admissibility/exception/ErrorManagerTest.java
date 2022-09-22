package it.gov.pagopa.admissibility.exception;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.controller.AdmissibilityController;
import it.gov.pagopa.admissibility.dto.ErrorDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
class ErrorManagerTest extends BaseIntegrationTest {

    @SpyBean
    AdmissibilityController admissibilityController;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void handleExceptionClientExceptionNoBody() {
        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionNoBody"))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build(""))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();
    }

    @Test
    void handleExceptionClientExceptionWithBody(){
        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionWithBody"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"));
        ErrorDTO errorClientExceptionWithBody= new ErrorDTO(Severity.ERROR,"Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("INITIATIVE1"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBody);

        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()));
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable = new ErrorDTO(Severity.ERROR,"Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("INITIATIVE1"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable);
    }

    @Test
    void handleExceptionClientExceptionTest(){
        ErrorDTO expectedErrorClientException = new ErrorDTO(Severity.ERROR,"Error","Something gone wrong");

        Mockito.when(admissibilityController.getInitiativeStatus("ClientException"))
                .thenThrow(ClientException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("INITIATIVE1"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);


        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionStatusAndMessage"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                       .build("INITIATIVE1"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

       Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionStatusAndMessageAndThrowable"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                       .build("INITIATIVE1"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);
    }

    @Test
    void handleExceptionRuntimeException(){
        ErrorDTO expectedErrorDefault = new ErrorDTO(Severity.ERROR,"Error","Something gone wrong");

        Mockito.when(admissibilityController.getInitiativeStatus("RuntimeException"))
                .thenThrow(RuntimeException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("INITIATIVE1"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
    }
}