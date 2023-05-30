package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OnboardingFamiliesRepositoryExt {
    Mono<UpdateResult> updateOnboardingFamilyOutcome(Family family, String initiativeId, OnboardingFamilyEvaluationStatus resultedStatus, List<OnboardingRejectionReason> resultedOnboardingRejectionReasons);
}
