package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.anpr.pagopa-pdnd-configuration.c001")
public class PagoPaAnprPdndConfig extends PdndInitiativeConfig {
}
