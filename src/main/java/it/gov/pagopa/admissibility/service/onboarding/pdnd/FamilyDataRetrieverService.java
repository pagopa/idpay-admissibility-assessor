package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

public interface FamilyDataRetrieverService {
    Mono<Family> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message);
}
