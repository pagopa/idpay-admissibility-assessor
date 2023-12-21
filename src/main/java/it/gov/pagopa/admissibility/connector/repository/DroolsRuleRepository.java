package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryExt;

/**
 * it will handle the persistence of {@link it.gov.pagopa.admissibility.model.DroolsRule} entity*/
public interface DroolsRuleRepository extends ReactiveMongoRepositoryExt<DroolsRule, String> {
}
