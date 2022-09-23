package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {InitiativeStatusDTO.class})
@Import(AdmissibilityControllerImpl.class)
class AdmissibilityControllerImplTest {

    @MockBean
    InitiativeStatusService initiativeStatusService;

    @MockBean
    ErrorNotifierService errorNotifierService;

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

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build(""))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(initiativeStatusService, Mockito.never()).getInitiativeStatusAndBudgetAvailable(Mockito.any());
    }
}