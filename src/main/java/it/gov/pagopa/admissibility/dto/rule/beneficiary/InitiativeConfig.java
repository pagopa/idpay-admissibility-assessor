package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pdndToken;
    private List<String> automatedCriteriaCodes;
    private BigDecimal initiativeBudget;
    private BigDecimal beneficiaryInitiativeBudget;
    private String status;
}
