package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

/**
 * It will check if the {@link OnboardingDTO#getUserId()} belong to an already onboarded/onboarding family, otherwise it will retrieve its family. <br />
 * If already exists an onboarding it will demand to {@link ExistentFamilyHandlerService} to handle the request, otherwise to {@link FamilyDataRetrieverFacadeService}
 * */
public interface OnboardingFamilyEvaluationService {
    Mono<EvaluationDTO> checkOnboardingFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, boolean retrieveFamily);
    Mono<EvaluationDTO> updateOnboardingFamilyOutcome(Family family, InitiativeConfig initiativeConfig, EvaluationDTO result);
    Mono<EvaluationDTO> retrieveAndCheckOnboardingFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, boolean retrieveFamily);
    void rescheduleRequestAnprLimitMessage(OnboardingDTO request, Message<String> message);
}
