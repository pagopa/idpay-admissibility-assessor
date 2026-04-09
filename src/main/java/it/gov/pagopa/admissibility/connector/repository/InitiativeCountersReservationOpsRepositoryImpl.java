package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

@Repository
@Slf4j
public class InitiativeCountersReservationOpsRepositoryImpl implements InitiativeCountersReservationOpsRepository {

    public static final String FIELD_ID = InitiativeCounters.Fields.id;
    public static final String FIELD_RESIDUAL_BUDGET_CENTS = InitiativeCounters.Fields.residualInitiativeBudgetCents;
    public static final String FIELD_UPDATE_DATE = InitiativeCounters.Fields.updateDate;
    public static final String FIELD_RESERVED_BUDGET_CENTS = InitiativeCounters.Fields.reservedInitiativeBudgetCents;
    public static final String FIELD_ONBOARDED = InitiativeCounters.Fields.onboarded;

    private final ReactiveMongoTemplate mongoTemplate;

    private final Clock clock;

    public InitiativeCountersReservationOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public Mono<InitiativeCounters> reserveBudget(String initiativeId, Long reservationCents) {
        log.trace("[ONBOARDING_REQUEST] [BUDGET_RESERVATION] Reserving budget {} on initiative {}", reservationCents, initiativeId);

        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(initiativeId)
                        .and(FIELD_RESIDUAL_BUDGET_CENTS).gte(reservationCents)
                ),
                new Update()
                        .inc(FIELD_ONBOARDED, 1L)
                        .inc(FIELD_RESERVED_BUDGET_CENTS,reservationCents)
                        .inc(FIELD_RESIDUAL_BUDGET_CENTS,-reservationCents)
                        .set(FIELD_UPDATE_DATE, Instant.now(clock)),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCounters.class
        );
    }

    public Mono<InitiativeCounters> deallocatedPartialBudget(String initiativeId, long deallocatedBudget) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(initiativeId)
                ),
                new Update()
                        .inc(FIELD_RESERVED_BUDGET_CENTS, -deallocatedBudget)
                        .inc(FIELD_RESIDUAL_BUDGET_CENTS, +deallocatedBudget)
                        .set(FIELD_UPDATE_DATE, Instant.now(clock)),

                FindAndModifyOptions.options().returnNew(true),
                InitiativeCounters.class
        );
    }

    public Mono<InitiativeCounters> deallocateBudget(String initiativeId, long deallocatedBudget) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(initiativeId)
                ),
                new Update()
                        .inc(FIELD_ONBOARDED, -1)
                        .inc(FIELD_RESERVED_BUDGET_CENTS, -deallocatedBudget)
                        .inc(FIELD_RESIDUAL_BUDGET_CENTS, +deallocatedBudget)
                        .set(FIELD_UPDATE_DATE, Instant.now(clock)),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCounters.class
        );
    }
}
