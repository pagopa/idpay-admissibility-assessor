package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

/** It will retrieve the {@link OnboardingDTO#getUserId()}'s family through {@link it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService} creating it if not exists:
 * <ul>
 *     <li>If it created it, it will return {@link Mono#empty()} in order to let the calling service to evaluate the request</li>
 *     <li>Otherwise it will let to {@link ExistentFamilyHandlerService} to handle it</li>
 * </ul>
 * */
public interface FamilyDataRetrieverFacadeService {
    Mono<EvaluationDTO> retrieveFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message);
}
