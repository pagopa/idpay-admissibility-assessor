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

    @JsonProperty("thresholdCode")
    private String thresholdCode;

    @JsonProperty("value")
    private List<ConsentValue> value;

    @JsonProperty("code")
    private String code;


    @Data
    @Builder
    static class ConsentValue {
        @JsonProperty("description")
        private String description;

        @JsonProperty("subDescription")
        private String subDescription;
    }
}

