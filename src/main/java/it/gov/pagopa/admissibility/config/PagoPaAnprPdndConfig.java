package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.anpr.pagopa-pdnd-configuration")
public class PagoPaAnprPdndConfig extends PdndInitiativeConfig {
}
