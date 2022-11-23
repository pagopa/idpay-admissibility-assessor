package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * Component that exposes APIs
 * */
@RequestMapping("/idpay/admissibility")
public interface AdmissibilityController {

    @GetMapping(value = "/initiative/{initiativeId}")
    Mono<InitiativeStatusDTO> getInitiativeStatus(@PathVariable("initiativeId") String initiativeId);
}
