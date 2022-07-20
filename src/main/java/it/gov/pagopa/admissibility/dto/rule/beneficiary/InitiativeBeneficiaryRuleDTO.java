package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import lombok.*;

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

}