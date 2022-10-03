package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SelfCriteriaMultiDTO.class, name = "multi"),
        @JsonSubTypes.Type(value = SelfCriteriaBoolDTO.class, name = "boolean")
})
public interface AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems {

}
