package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/** It will translate an initiative into a DroolsRule, returning null if invalid */ // TODO handle null return
public interface BeneficiaryRule2DroolsRule extends Function<Flux<Initiative2BuildDTO>, Flux<DroolsRule>> {
}
