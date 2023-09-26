package it.gov.pagopa.admissibility.connector.rest.pdnd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.pdnd.config")
@Data
public class PdndConfig {
    private String audience;
}
