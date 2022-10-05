package it.gov.pagopa.admissibility.service.build.filter;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;

import java.util.function.Predicate;

/**
 * Filter to skip {@link Initiative2BuildDTO}
 * */
public interface BeneficiaryRuleFilter extends Predicate<Initiative2BuildDTO> {
}