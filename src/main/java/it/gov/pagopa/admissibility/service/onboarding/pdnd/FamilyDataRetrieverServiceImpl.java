package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    @Override
    public Mono<Family> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND re-scheduling if dailyLimit occurred
        return null;
    }
}
