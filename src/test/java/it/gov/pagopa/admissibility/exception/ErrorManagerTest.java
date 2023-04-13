package it.gov.pagopa.admissibility.exception;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.controller.AdmissibilityController;
import it.gov.pagopa.admissibility.dto.ErrorDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class ErrorManagerTest extends BaseIntegrationTest {

    @SpyBean
    AdmissibilityController admissibilityController;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void handleExceptionClientExceptionNoBody() {
        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionNoBody"))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"NOTFOUND"));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("ClientExceptionNoBody"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();
    }

    @Test
    void handleExceptionClientExceptionWithBody(){
        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionWithBody"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"));
        ErrorDTO errorClientExceptionWithBody= new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("ClientExceptionWithBody"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBody);

        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()));
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable = new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable);
    }

    @Test
    void handleExceptionClientExceptionTest(){
        ErrorDTO expectedErrorClientException = new ErrorDTO("Error","Something gone wrong");

        Mockito.when(admissibilityController.getInitiativeStatus("ClientException"))
                .thenThrow(ClientException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("ClientException"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);


        Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionStatusAndMessage"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                       .build("ClientExceptionStatusAndMessage"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

       Mockito.when(admissibilityController.getInitiativeStatus("ClientExceptionStatusAndMessageAndThrowable"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                       .build("ClientExceptionStatusAndMessageAndThrowable"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);
    }

    @Test
    void handleExceptionRuntimeException(){
        ErrorDTO expectedErrorDefault = new ErrorDTO("Error","Something gone wrong");

        Mockito.when(admissibilityController.getInitiativeStatus("RuntimeException"))
                .thenThrow(RuntimeException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("RuntimeException"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
    }
}