package it.gov.pagopa.admissibility.service.build.filter;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Order(0)
public class BeneficiaryListFilter implements BeneficiaryRuleFilter{
    @Override
    public boolean test(Initiative2BuildDTO initiative2BuildDTO) {
        boolean isPresentBeneficiaryRule = initiative2BuildDTO.getBeneficiaryRule() != null;
        log.info("Beneficiary is present in initiative with id %s: %b".formatted(initiative2BuildDTO.getInitiativeId(), isPresentBeneficiaryRule));
        return isPresentBeneficiaryRule;
    }
}