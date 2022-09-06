package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
@Slf4j
public class InitiativeCountersReservationOpsRepositoryImpl implements InitiativeCountersReservationOpsRepository {

    public static final String FIELD_ID = InitiativeCounters.Fields.id;
    public static final String FIELD_RESIDUAL_BUDGET_CENTS = InitiativeCounters.Fields.residualInitiativeBudgetCents;
    public static final String FIELD_RESERVED_BUDGET_CENTS = InitiativeCounters.Fields.reservedInitiativeBudgetCents;
    public static final String FIELD_ONBOARDED = InitiativeCounters.Fields.onboarded;

    private final ReactiveMongoTemplate mongoTemplate;

    public InitiativeCountersReservationOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<InitiativeCounters> reserveBudget(String initiativeId, BigDecimal reservation) {
        log.trace("[ONBOARDING_REQUEST] [BUDGET_RESERVATION] Reserving budget {} on initiative {}", reservation, initiativeId);
        long reservationCents = reservation.longValue() * 100;

        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(initiativeId)
                        .and(FIELD_RESIDUAL_BUDGET_CENTS).gte(reservationCents)
                ),
                new Update()
                        .inc(FIELD_ONBOARDED, 1L)
                        .inc(FIELD_RESERVED_BUDGET_CENTS,reservationCents)
                        .inc(FIELD_RESIDUAL_BUDGET_CENTS,-reservationCents),
                FindAndModifyOptions.options().returnNew(true),
        InitiativeCounters.class
        );
    }
}
