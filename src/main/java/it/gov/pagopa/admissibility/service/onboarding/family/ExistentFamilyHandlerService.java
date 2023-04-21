package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

public interface ExistentFamilyHandlerService {
    Mono<EvaluationDTO> handleExistentFamily(OnboardingDTO onboardingRequest, OnboardingFamilies family, Message<String> message);
}
