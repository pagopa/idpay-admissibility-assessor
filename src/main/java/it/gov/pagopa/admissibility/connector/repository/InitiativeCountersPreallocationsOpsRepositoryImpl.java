package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCountersPreallocations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
}
