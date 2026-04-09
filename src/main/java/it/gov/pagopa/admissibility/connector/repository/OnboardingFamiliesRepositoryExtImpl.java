package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class OnboardingFamiliesRepositoryExtImpl implements OnboardingFamiliesRepositoryExt {

    private final ReactiveMongoTemplate mongoTemplate;
    private final Clock clock;
    public OnboardingFamiliesRepositoryExtImpl(ReactiveMongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    @Override
    public Mono<UpdateResult> updateOnboardingFamilyOutcome(Family family, String initiativeId, OnboardingFamilyEvaluationStatus resultedStatus, List<OnboardingRejectionReason> resultedOnboardingRejectionReasons) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where(OnboardingFamilies.Fields.id).is(OnboardingFamilies.buildId(family, initiativeId))),
                new Update()
                        .set(OnboardingFamilies.Fields.status, resultedStatus)
                        .set(OnboardingFamilies.Fields.onboardingRejectionReasons, resultedOnboardingRejectionReasons)
                        .set(OnboardingFamilies.Fields.updateDate, Instant.now(clock)),
                OnboardingFamilies.class
        );
    }

    @Override
    public Flux<OnboardingFamilies> findByInitiativeIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(OnboardingFamilies.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, OnboardingFamilies.class);
    }
}