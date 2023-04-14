package it.gov.pagopa.admissibility.dto.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Initiative2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String pdndToken;
    private String status;
    private InitiativeBeneficiaryRuleDTO beneficiaryRule;
    private InitiativeGeneralDTO general;
    private InitiativeAdditionalInfoDTO additionalInfo;
    private String initiativeRewardType;

}
