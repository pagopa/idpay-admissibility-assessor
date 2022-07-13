package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import lombok.*;

/**
 * SelfCriteriaMultiDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class SelfCriteriaMultiDTO  implements AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems {

    @JsonProperty("_type")
    private FieldEnumOnboardingDTO _type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("value")
    private List<String> value;

    @JsonProperty("code")
    private String code;

}
