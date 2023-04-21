package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

public interface FamilyDataRetrieverFacadeService {
    Mono<EvaluationDTO> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message);
}
