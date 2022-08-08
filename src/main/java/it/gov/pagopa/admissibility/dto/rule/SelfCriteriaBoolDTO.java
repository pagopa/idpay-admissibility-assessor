package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * SelfCriteriaBoolDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class SelfCriteriaBoolDTO  implements AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems {

    @JsonProperty("_type")
    private FieldEnumOnboardingDTO _type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("value")
    private Boolean value;

    @JsonProperty("code")
    private String code;
}
