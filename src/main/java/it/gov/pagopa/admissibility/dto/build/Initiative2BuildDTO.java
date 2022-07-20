package it.gov.pagopa.admissibility.dto.build;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import lombok.Data;

@Data
public class Initiative2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private InitiativeBeneficiaryRuleDTO beneficiaryRule;
}
