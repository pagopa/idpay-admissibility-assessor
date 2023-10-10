package it.gov.pagopa.common.reactive.pdnd.config;

import lombok.Data;
import org.springframework.http.HttpMethod;

@Data
public abstract class BasePdndServiceConfig {
    private String audience;
    private HttpMethod httpMethod;
    private String path;
}
