package it.gov.pagopa.dto;

import lombok.Data;

import java.util.List;

@Data
public class InitiativeConfig {

    private String initiativeId;
    private List<String> automatedCriteriaCodes;
}
