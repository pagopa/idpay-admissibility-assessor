package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponse;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RequestMapping("/idpay/admissibility")
public interface TestConnectionController {
    @GetMapping(value = "/inps/connection/test/threshold/{threshold}")
    Mono<Optional<List<OnboardingRejectionReason>>> getThreshold(@PathVariable("threshold") String threshold, @RequestHeader("X-User-Code") String userCode);

    @GetMapping(value = "/inps/connection/test/threshold/{threshold}/v2")
    Mono<ConsultazioneSogliaIndicatoreResponse> getThresholdV2(@PathVariable("threshold") String threshold, @RequestHeader("X-User-Code") String userCode);
}
