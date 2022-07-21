package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InitiativeConfig {

    private String initiativeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pdndToken;
    private List<String> automatedCriteriaCodes;
}
