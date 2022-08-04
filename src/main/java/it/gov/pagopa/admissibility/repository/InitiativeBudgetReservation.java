package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class InitiativeBudgetReservation {

    public static final String FIELD_INITIATIVE_BUDGET = "%s.%s".formatted(DroolsRule.Fields.initiativeConfig, InitiativeConfig.Fields.initiativeBudget);
    public static final String FIELD_RESERVED_BUDGET = "%s.%s".formatted(DroolsRule.Fields.initiativeConfig, InitiativeConfig.Fields.reservedInitiativeBudget);
    public static final String FIELD_BENEFICIARY_BUDGET = "%s.%s".formatted(DroolsRule.Fields.initiativeConfig, InitiativeConfig.Fields.beneficiaryInitiativeBudget);
    public static final String FIELD_ONBOARDED_FIELD = "%s.%s".formatted(DroolsRule.Fields.initiativeConfig, InitiativeConfig.Fields.onboarded);

    private final ReactiveMongoTemplate mongoTemplate;

    public InitiativeBudgetReservation(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<DroolsRule> reserveBudget(String initiativeId) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where("id").is(initiativeId)
                        .and("$expr").gt(
                                List.of(
                                        FIELD_INITIATIVE_BUDGET,
                                        ArithmeticOperators.Add
                                                .valueOf(FIELD_RESERVED_BUDGET)
                                                .add(FIELD_BENEFICIARY_BUDGET)
                                                .toDocument()
                                )
                        )
                ),
                new Update()
                        .inc(FIELD_ONBOARDED_FIELD, 1L)
                        .set(FIELD_RESERVED_BUDGET,
                                ArithmeticOperators.Add
                                        .valueOf(FIELD_RESERVED_BUDGET)
                                        .add(FIELD_BENEFICIARY_BUDGET)),
                DroolsRule.class
        );
    }
}
