package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsThresholdRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class TestConnectionControllerImpl implements TestConnectionController{
    private final InpsThresholdRetrieverService inpsThresholdRetrieverService;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    public TestConnectionControllerImpl(InpsThresholdRetrieverService inpsThresholdRetrieverService, PagoPaAnprPdndConfig pagoPaAnprPdndConfig) {
        this.inpsThresholdRetrieverService = inpsThresholdRetrieverService;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> getThreshold(String threshold, String userCode) {
        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(false, Collections.emptyList(), false, false, true, threshold);
        return inpsThresholdRetrieverService.invoke(userCode,pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c001"), pdndServicesInvocation,  new OnboardingDTO());

    }
}
