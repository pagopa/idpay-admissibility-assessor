package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/idpay/admissibility")
public interface TestConnectionController {
    @GetMapping(value = "/inps/connection/test/threshold/{threshold}")
    Mono<ConsultazioneSogliaIndicatoreResponse> getThreshold(@PathVariable("threshold") String threshold, @RequestHeader("X-User-Code") String userCode, @RequestHeader("X-Date") String date);
}
