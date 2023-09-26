package it.gov.pagopa.admissibility.connector.rest.pdnd.config;

import lombok.Data;

@Data
public abstract class BaseAgidConfig {
    private String env;
    private String userId;
}
