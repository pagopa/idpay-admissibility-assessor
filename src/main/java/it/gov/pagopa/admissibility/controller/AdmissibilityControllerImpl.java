package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.exception.ClientExceptionWithBody;
import it.gov.pagopa.admissibility.service.InitiativeStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Slf4j
public class AdmissibilityControllerImpl implements AdmissibilityController{

    private final InitiativeStatusService initiativeStatusService;

    public AdmissibilityControllerImpl(InitiativeStatusService initiativeStatusService) {
        this.initiativeStatusService = initiativeStatusService;
    }

    @Override
    public Flux<InitiativeStatusDTO> getInitiativeStatus(String initiativeId) {
        if(StringUtils.hasText(initiativeId)) {
            return initiativeStatusService.getInitiativeStatusAndBudgetAvailable(initiativeId);
        } else {
            throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error", "Field initiativeId is mandatory");
        }
    }
}
