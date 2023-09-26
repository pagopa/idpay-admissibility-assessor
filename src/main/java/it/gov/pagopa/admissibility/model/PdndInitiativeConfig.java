package it.gov.pagopa.admissibility.model;

import lombok.Data;

@Data
public class PdndInitiativeConfig {
    private final String clientId;
    private final String kid;
    private final String purposeId;
}
