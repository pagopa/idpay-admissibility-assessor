package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * InitiativeBeneficiaryRuleDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class InitiativeBeneficiaryRuleDTO   {

    @JsonProperty("selfDeclarationCriteria")
    private List<AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems> selfDeclarationCriteria;
    @JsonProperty("automatedCriteria")
    private List<AutomatedCriteriaDTO> automatedCriteria;
    /**
     * PDND Key/Token Id
     */
    @JsonProperty("apiKeyClientId")
    private String apiKeyClientId;
    /**
     * PDND Key/Token Assertion
     */
    @JsonProperty("apiKeyClientAssertion")
    private String apiKeyClientAssertion;

}