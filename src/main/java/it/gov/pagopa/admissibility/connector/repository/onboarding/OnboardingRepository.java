package it.gov.pagopa.admissibility.connector.repository.onboarding;

import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import it.gov.pagopa.admissibility.model.onboarding.OnboardingFamilyInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Repository
public interface OnboardingRepository extends ReactiveMongoRepository<Onboarding, String> {
    Flux<OnboardingFamilyInfo> findByIdInAndStatus(Set<String> onboardingIds, String status);
    Mono<Onboarding> findById(String onboardingId);
}
