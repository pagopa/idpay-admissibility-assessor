package it.gov.pagopa.admissibility.service.drools.transformer.admissibility;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public interface BeneficiaryRule2DroolsRule extends Function<Flux<InitiativeBeneficiaryRuleDTO>, Flux<DroolsRule>> {
}
