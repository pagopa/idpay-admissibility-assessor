package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Slf4j
public class AdmissibilityControllerImpl implements AdmissibilityController{



    @Override
    public Flux<InitiativeStatusDTO> getInitiativeStatus(String initiativeId) {
        if(initiativeId != null) {
            return null;    // TODO InitiativeStatusDTO
        } else {
            return null;    // TODO ERROR
        }
    }
}
