package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * it will handle the persistence of {@link OnboardingFamilies} entity*/
public interface OnboardingFamiliesRepository extends ReactiveMongoRepository<OnboardingFamilies, String>, OnboardingFamiliesRepositoryExt {
    Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingFamiliesRepository.class);

    Flux<OnboardingFamilies> findByMemberIdsInAndInitiativeId(String memberId, String initiativeId);

    /** it will create if not exists a new {@link it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus#IN_PROGRESS} if the provided id not exists. If it doesn't exist, it will return empty */
    default Mono<OnboardingFamilies> createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(Family family, String initiativeId) {
        OnboardingFamilies onboardingInProgress = OnboardingFamilies.builder(family, initiativeId)
                .status(OnboardingFamilyEvaluationStatus.IN_PROGRESS)
                .createDate(LocalDateTime.now())
                .build();

        return insert(onboardingInProgress)
                .onErrorResume(DuplicateKeyException.class, e-> {
                    log.debug("[ONBOARDING_REQUEST] Onboarding Family having id {} already exists with the following memberIds {}", family.getFamilyId(), family.getMemberIds());
                    return Mono.empty();
                });
    }
}
