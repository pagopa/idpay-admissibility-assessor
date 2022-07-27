package it.gov.pagopa.admissibility.dto.build;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeGeneralDTO;
import lombok.Data;

@Data
public class Initiative2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private String pdndToken;
    private InitiativeBeneficiaryRuleDTO beneficiaryRule;
    private InitiativeGeneralDTO general;
}
