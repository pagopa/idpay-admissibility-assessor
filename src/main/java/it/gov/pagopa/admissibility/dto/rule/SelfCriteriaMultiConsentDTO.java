package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * SelfCriteriaMultiDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class SelfCriteriaMultiConsentDTO implements AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems {

    @JsonProperty("description")
    private String description;

    @JsonProperty("subDescription")
    private String subDescription;

    @JsonProperty("value")
    private List<ConsentValue> value;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConsentValue {
        @JsonProperty("description")
        private String description;

        @JsonProperty("subDescription")
        private String subDescription;

        @JsonProperty("code")
        private String code;

        @JsonProperty("beneficiaryBudgetMinCents")
        private Long beneficiaryBudgetCentsMin;

        @JsonProperty("beneficiaryBudgetMaxCents")
        private Long beneficiaryBudgetCentsMax;

        @JsonProperty("thresholdCode")
        private String thresholdCode;
    }
}

