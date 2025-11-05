package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

/**
 * It will handle the use case where already {@link OnboardingDTO#getUserId()}'s family onboarding request has already been performed:
 * <ul>
 *     <li>If the request is IN_PROGRESS, it will reschedule the message, returning {@link Mono#empty()}</li>
 *     <li>Otherwise it will return a {@link EvaluationDTO} configured with the same result built for the previous family members</li>
 * </ul> */
public interface ExistentFamilyHandlerService {
    Mono<EvaluationDTO> handleExistentFamily(OnboardingDTO onboardingRequest, OnboardingFamilies family, InitiativeConfig initiativeConfig, Message<String> message);
    Mono<EvaluationDTO> mapFamilyOnboardingResult(OnboardingDTO onboardingRequest, OnboardingFamilies family, InitiativeConfig initiativeConfig);
    Mono<EvaluationDTO> mapFamilyMemberAlreadyOnboardingResult(OnboardingDTO onboardingRequest, String familyId, InitiativeConfig initiativeConfig);
}
