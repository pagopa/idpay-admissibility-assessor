package it.gov.pagopa.admissibility.model;

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
    private LocalDate startDate;
    private LocalDate endDate;
    private String pdndToken;
    private List<String> automatedCriteriaCodes;
    private BigDecimal initiativeBudget;
    private BigDecimal beneficiaryInitiativeBudget;
}