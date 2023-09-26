package it.gov.pagopa.admissibility.connector.rest.pdnd.dto;

import it.gov.pagopa.admissibility.connector.rest.pdnd.config.BaseAgidConfig;
import lombok.Data;

@Data
public class PdndServiceConfig {
    private BaseAgidConfig agidConfig;
    private String audience;
    private long authExpirationSeconds;
}
