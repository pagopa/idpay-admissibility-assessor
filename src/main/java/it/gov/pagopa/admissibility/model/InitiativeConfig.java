package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String organizationName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String apiKeyClientId;
    private String apiKeyClientAssertion;
    // TODO PdndInitiativeConfig
    private List<AutomatedCriteriaDTO> automatedCriteria;
    private List<String> automatedCriteriaCodes;
    private BigDecimal initiativeBudget;
    private BigDecimal beneficiaryInitiativeBudget;
    private boolean rankingInitiative;
    private List<Order> rankingFields;
    private String initiativeRewardType;
    private Boolean isLogoPresent;
    private InitiativeGeneralDTO.BeneficiaryTypeEnum beneficiaryType;
}
