package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OnboardingFamiliesRepositoryExt {
    Mono<UpdateResult> updateOnboardingFamilyOutcome(Family family, String initiativeId, OnboardingFamilyEvaluationStatus resultedStatus, List<OnboardingRejectionReason> resultedOnboardingRejectionReasons);
    Flux<OnboardingFamilies> deletePaged(String initiativeId, int pageSize);
    Flux<OnboardingFamilies> findByInitiativeId(String initiativeId, int batchSize);
}
