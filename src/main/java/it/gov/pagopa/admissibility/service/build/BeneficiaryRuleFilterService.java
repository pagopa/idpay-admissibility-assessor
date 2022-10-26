package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.service.build.filter.BeneficiaryRuleFilter;

/**
 * This component will take a {@link  Initiative2BuildDTO} and will test an against all the {@link BeneficiaryRuleFilter} configured
 * */
public interface BeneficiaryRuleFilterService {
    Boolean filter(Initiative2BuildDTO initiative2BuildDTO);
}