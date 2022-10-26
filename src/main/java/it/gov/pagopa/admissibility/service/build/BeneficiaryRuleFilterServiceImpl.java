package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.service.build.filter.BeneficiaryRuleFilter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficiaryRuleFilterServiceImpl implements BeneficiaryRuleFilterService {

    private final List<BeneficiaryRuleFilter> filters;

    public BeneficiaryRuleFilterServiceImpl(List<BeneficiaryRuleFilter> filters) {
        this.filters = filters;
    }

    @Override
    public Boolean filter(Initiative2BuildDTO initiative2BuildDTO) {
        return filters.stream().allMatch(f -> f.test(initiative2BuildDTO));
    }
}