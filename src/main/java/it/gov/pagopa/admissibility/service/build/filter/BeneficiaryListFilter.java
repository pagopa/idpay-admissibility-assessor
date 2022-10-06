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
        if (!isPresentBeneficiaryRule){
            log.info("Initiative containing beneficiary list: {}",initiative2BuildDTO.getInitiativeId());
        }
        return isPresentBeneficiaryRule;
    }
}