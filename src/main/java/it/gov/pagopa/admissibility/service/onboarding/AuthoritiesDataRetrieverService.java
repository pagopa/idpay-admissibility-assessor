package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

/**
 * Given an {@link OnboardingDTO} it will fetch the authorities' data used by the {@link OnboardingDTO#getInitiativeId()}
 * returning an empty Mono  if it was not able to retrieve the data due to the overflows the daily limit, rescheduling itself the message
 */
public interface AuthoritiesDataRetrieverService {

    Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingDTO, InitiativeConfig initiativeConfig, Message<String> message);
}
