package it.gov.pagopa.admissibility.connector.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface PdndDataRetrieverService {
    Mono<Optional<List<OnboardingRejectionReason>>> MONO_OPTIONAL_EMPTY_LIST = Mono.just(Optional.of(Collections.emptyList()));

    /** It will invoke PDND services returning an Optional.empty if it needs a rescheduling  */
    Mono<Optional<List<OnboardingRejectionReason>>> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig, PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest);
}
