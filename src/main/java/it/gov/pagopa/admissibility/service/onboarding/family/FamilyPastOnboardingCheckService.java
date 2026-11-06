package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import reactor.core.publisher.Mono;

/**
 * Checks whether an adult family member already used a past edition of the same bonus.
 */
public interface FamilyPastOnboardingCheckService {

    /**
     * @return an ONBOARDING_KO evaluation with FAMILY_BONUS_ALREADY_USED, or empty when the check is disabled or passes
     */
    Mono<EvaluationDTO> checkFamilyPastOnboardings(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig);
}

