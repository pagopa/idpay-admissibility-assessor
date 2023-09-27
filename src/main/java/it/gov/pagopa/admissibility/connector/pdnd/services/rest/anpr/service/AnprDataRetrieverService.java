package it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverServiceImpl;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface AnprDataRetrieverService {
    /** It will invoke ANPR services returning an Optional.empty if it needs a rescheduling  */
    Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            AuthoritiesDataRetrieverServiceImpl.PdndServicesInvocation pdndServicesInvocation,
            OnboardingDTO onboardingRequest);
}
