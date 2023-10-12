package it.gov.pagopa.common.reactive.pdnd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** PDND Authorization Server configuration */
@Configuration
@ConfigurationProperties(prefix = "app.pdnd.config")
@Data
public class PdndConfig {
    private String audience;
}
