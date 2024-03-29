package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;

import java.util.function.Function;

/** It will translate an initiative into a DroolsRule, returning null if invalid */
public interface BeneficiaryRule2DroolsRule extends Function<Initiative2BuildDTO, DroolsRule> {
}
