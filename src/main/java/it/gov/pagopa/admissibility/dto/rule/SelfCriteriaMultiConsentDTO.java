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

    @JsonProperty("code")
    private String code;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConsentValue {
        @JsonProperty("description")
        private String description;

        @JsonProperty("subDescription")
        private String subDescription;

        @JsonProperty("value")
        private String value;

        @JsonProperty("verify")
        private boolean verify;

        @JsonProperty("beneficiaryBudgetCentsMin")
        private Long beneficiaryBudgetCentsMin;

        @JsonProperty("beneficiaryBudgetCentsMax")
        private Long beneficiaryBudgetCentsMax;

        @JsonProperty("thresholdCode")
        private String thresholdCode;

        @JsonProperty("blockingVerify")
        private boolean blockingVerify;
    }
}

