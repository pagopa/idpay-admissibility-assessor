package it.gov.pagopa.admissibility.dto.rule;

import lombok.Data;

@Data
public class Initiative2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private String pdndToken;
    private InitiativeBeneficiaryRuleDTO beneficiaryRule;
    private InitiativeGeneralDTO general;
}
