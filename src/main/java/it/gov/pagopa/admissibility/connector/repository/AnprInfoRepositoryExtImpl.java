package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.AnprInfo;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class AnprInfoRepositoryExtImpl implements AnprInfoRepositoryExt{

    private final ReactiveMongoTemplate mongoTemplate;

    public AnprInfoRepositoryExtImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<AnprInfo> findByInitiativeIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(AnprInfo.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, AnprInfo.class);
    }
}
