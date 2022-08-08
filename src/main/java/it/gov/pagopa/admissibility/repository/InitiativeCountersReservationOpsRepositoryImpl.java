package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class InitiativeCountersReservationOpsRepositoryImpl implements InitiativeCountersReservationOpsRepository {

    public static final String FIELD_INITIATIVE_BUDGET = InitiativeCounters.Fields.initiativeBudget;
    public static final String FIELD_RESERVED_BUDGET = InitiativeCounters.Fields.reservedInitiativeBudget;
    public static final String FIELD_ONBOARDED_FIELD = InitiativeCounters.Fields.onboarded;

    private final ReactiveMongoTemplate mongoTemplate;

    public InitiativeCountersReservationOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<InitiativeCounters> reserveBudget(String initiativeId, BigDecimal reservation) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where("id").is(initiativeId)
                        .and("$expr").gt(
                                List.of(
                                        "$"+FIELD_INITIATIVE_BUDGET,
                                        ArithmeticOperators.Add
                                                .valueOf(FIELD_RESERVED_BUDGET)
                                                .add(reservation)
                                                .toDocument()
                                )
                        )
                ),
                new Update()
                        .inc(FIELD_ONBOARDED_FIELD, 1L)
                        .inc(FIELD_RESERVED_BUDGET,reservation),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCounters.class
        );
    }
}
