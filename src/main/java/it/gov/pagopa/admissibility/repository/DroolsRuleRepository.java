package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.DroolsRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * it will handle the persistence of {@link it.gov.pagopa.admissibility.model.DroolsRule} entity*/
public interface DroolsRuleRepository extends ReactiveMongoRepository<DroolsRule, String> {
}
