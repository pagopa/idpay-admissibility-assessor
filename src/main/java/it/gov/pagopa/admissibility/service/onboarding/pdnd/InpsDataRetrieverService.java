package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverServiceImpl;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface InpsDataRetrieverService {

    /** It will invoke INPS services returning an Optional.empty if it needs a rescheduling  */
    Mono<Optional<List<OnboardingRejectionReason>>> invoke(String fiscalCode, IseeTypologyEnum iseeType, AuthoritiesDataRetrieverServiceImpl.PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest);
}
