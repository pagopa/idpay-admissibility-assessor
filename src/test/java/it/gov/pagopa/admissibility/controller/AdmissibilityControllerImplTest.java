package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.ErrorDTO;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.exception.Severity;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

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
                .thenReturn(Flux.just(is));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build(initiativeId))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(InitiativeStatusDTO.class).contains(is);

        Mockito.verify(initiativeStatusService, Mockito.times(1)).getInitiativeStatusAndBudgetAvailable(Mockito.any());
    }

    @Test
    void getInitiativeStatusBadRequest(){

        String initiativeId = null;

        ErrorDTO expectedErrorDTO = new ErrorDTO(Severity.ERROR,"Error", "Field initiativeId is mandatory");

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/admissibility/initiative/{initiativeId}")
                        .build(""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDTO);

        Mockito.verify(initiativeStatusService, Mockito.never()).getInitiativeStatusAndBudgetAvailable(Mockito.any());
    }
}