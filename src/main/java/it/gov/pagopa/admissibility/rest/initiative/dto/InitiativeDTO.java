package it.gov.pagopa.admissibility.rest.initiative.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiativeDTO {
    @JsonProperty("initiativeId")
    private String initiativeId;

    @JsonProperty("initiativeName")
    private String initiativeName;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("pdndToken")
    private String pdndToken;

    @JsonProperty("status")
    private String status;

    @JsonProperty("pdndCheck")
    private Boolean pdndCheck;

    @JsonProperty("autocertificationCheck")
    private Boolean autocertificationCheck;


    @JsonProperty("general")
    private InitiativeGeneralDTO general;

//    @JsonProperty("additionalInfo")
//    private InitiativeAdditionalDTO additionalInfo = null;

    @JsonProperty("beneficiaryRule")
    private InitiativeBeneficiaryRuleDTO beneficiaryRule;
//
//    @JsonProperty("legal")
//    private InitiativeLegalDTO legal;
}
