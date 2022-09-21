package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

/**
 * Component that exposes APIs
 * */
@RequestMapping("/idpay/admissibility")
public interface AdmissibilityController {

    @GetMapping(value = "/initiative/{initiativeId}")
    Flux<InitiativeStatusDTO> getInitiativeStatus(String initiativeId);
}
