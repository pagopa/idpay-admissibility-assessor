package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
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
                .switchIfEmpty(Mono.error(new ClientExceptionWithBody(HttpStatus.NOT_FOUND, OnboardingConstants.ExceptionCode.INITIATIVE_NOT_FOUND, "The initiative with id %s does not exist".formatted(initiativeId))));
    }
}
