package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

@Slf4j
public class CustomSequenceGeneratorOpsRepositoryImpl implements CustomSequenceGeneratorOpsRepository {

    public static final String FIELD_ID = CustomSequenceGenerator.Fields.id;
    public static final String FIELD_SEQUENCE_VALUE = CustomSequenceGenerator.Fields.opClientIdSequence;

    private final ReactiveMongoTemplate mongoTemplate;

    public CustomSequenceGeneratorOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Long> getSequence(String sequenceId) {
        log.trace("[ONBOARDING_REQUEST][GENERATE_SEQUENCE] Generate next sequence value");

        return  mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(sequenceId)
                ),
                new Update()
                        .inc(FIELD_SEQUENCE_VALUE, 1L),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                CustomSequenceGenerator.class)
                .map(CustomSequenceGenerator::getOpClientIdSequence);
    }
}
