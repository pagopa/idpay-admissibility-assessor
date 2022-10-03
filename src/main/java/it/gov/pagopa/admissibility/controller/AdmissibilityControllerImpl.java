package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.exception.ClientExceptionNoBody;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class AdmissibilityControllerImpl implements AdmissibilityController{

    private final InitiativeStatusService initiativeStatusService;

    public AdmissibilityControllerImpl(InitiativeStatusService initiativeStatusService) {
        this.initiativeStatusService = initiativeStatusService;
    }

    @Override
    public Mono<InitiativeStatusDTO> getInitiativeStatus(String initiativeId) {
            return initiativeStatusService.getInitiativeStatusAndBudgetAvailable(initiativeId)
                    .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }
}
