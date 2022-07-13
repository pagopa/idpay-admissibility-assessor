package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import lombok.Data;

import java.util.List;

@Data
public class InitiativeConfig {

    private String initiativeId;
    private List<String> automatedCriteriaCodes;
}
