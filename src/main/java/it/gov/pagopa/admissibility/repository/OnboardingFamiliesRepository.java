package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * it will handle the persistence of {@link OnboardingFamilies} entity*/
public interface OnboardingFamiliesRepository extends ReactiveMongoRepository<OnboardingFamilies, String>, OnboardingFamiliesRepositoryExt {
    Flux<OnboardingFamilies> findByMemberIdsIn(String memberId);
}
