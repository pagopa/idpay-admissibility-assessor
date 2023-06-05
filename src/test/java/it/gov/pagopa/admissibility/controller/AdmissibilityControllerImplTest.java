package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {AdmissibilityControllerImpl.class})
class AdmissibilityControllerImplTest {

    @MockBean
    InitiativeStatusService initiativeStatusService;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void getInitiativeStatusOk() {
        String initiativeId = "INITIATIVE1";

        InitiativeStatusDTO is = new InitiativeStatusDTO("STATUS1", true);

        //initiativeId present in request
        Mockito.when(initiativeStatusService.getInitiativeStatusAndBudgetAvailable(initiativeId))
                .thenReturn(Mono.just(is));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build(initiativeId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(InitiativeStatusDTO.class).isEqualTo(is);

        Mockito.verify(initiativeStatusService, Mockito.times(1)).getInitiativeStatusAndBudgetAvailable(Mockito.any());
    }

    @Test
    void getInitiativeStatusNotFound(){

        Mockito.when(initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1"))
                .thenReturn(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "NOTFOUND")));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build("INITIATIVE1"))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(initiativeStatusService, Mockito.times(1)).getInitiativeStatusAndBudgetAvailable(Mockito.any());
    }
}