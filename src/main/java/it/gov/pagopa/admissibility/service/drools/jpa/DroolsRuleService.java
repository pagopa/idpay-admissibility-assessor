package it.gov.pagopa.admissibility.service.drools.jpa;

import it.gov.pagopa.admissibility.model.DroolsRule;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DroolsRuleService {
    Mono<DroolsRule> save(DroolsRule droolsRule);
    Flux<DroolsRule> findAll();
}
