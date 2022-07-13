package it.gov.pagopa.dto;

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
        @JsonSubTypes.Type(value = SelfCriteriaMultiDTO.class, name = "SelfCriteriaMultiDTO"),
        @JsonSubTypes.Type(value = SelfCriteriaBoolDTO.class, name = "SelfCriteriaBoolDTO")
})
public interface AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems {

}
