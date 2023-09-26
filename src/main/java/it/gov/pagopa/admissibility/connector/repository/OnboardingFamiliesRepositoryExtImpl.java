package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class OnboardingFamiliesRepositoryExtImpl implements OnboardingFamiliesRepositoryExt {

    private final ReactiveMongoTemplate mongoTemplate;

    public OnboardingFamiliesRepositoryExtImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UpdateResult> updateOnboardingFamilyOutcome(Family family, String initiativeId, OnboardingFamilyEvaluationStatus resultedStatus, List<OnboardingRejectionReason> resultedOnboardingRejectionReasons) {
        return mongoTemplate.updateFirst(
                new Query(Criteria.where(OnboardingFamilies.Fields.id).is(OnboardingFamilies.buildId(family, initiativeId))),
                new Update()
                        .set(OnboardingFamilies.Fields.status, resultedStatus)
                        .set(OnboardingFamilies.Fields.onboardingRejectionReasons, resultedOnboardingRejectionReasons),
                OnboardingFamilies.class
        );
    }

    @Override
    public Flux<OnboardingFamilies> deletePaged(String initiativeId, int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return mongoTemplate.findAllAndRemove(
                Query.query(Criteria.where(OnboardingFamilies.Fields.initiativeId).is(initiativeId)).with(pageable),
                OnboardingFamilies.class
        );
    }

    @Override
    public Flux<OnboardingFamilies> findByInitiativeId(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(OnboardingFamilies.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, OnboardingFamilies.class);
    }
}