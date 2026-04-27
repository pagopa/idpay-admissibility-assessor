package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCountersPreallocations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

public class InitiativeCountersPreallocationsOpsRepositoryImpl implements InitiativeCountersPreallocationsOpsRepository{
    private final ReactiveMongoTemplate mongoTemplate;

    public InitiativeCountersPreallocationsOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Boolean> deleteByIdReturningResult(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, InitiativeCountersPreallocations.class)
                .map(result -> result.getDeletedCount() > 0);
    }

    @Override
    public Mono<Boolean> updatePreallocatedAmount(String id, long finalBudget) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("preallocatedAmountCents", finalBudget);

        return mongoTemplate
                .updateFirst(query, update, InitiativeCountersPreallocations.class)
                .map(result -> result.getModifiedCount() > 0);
    }
}
