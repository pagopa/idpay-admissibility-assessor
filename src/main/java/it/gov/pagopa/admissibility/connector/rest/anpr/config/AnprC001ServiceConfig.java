package it.gov.pagopa.admissibility.connector.rest.anpr.config;

import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.anpr.c001-consultazione-anpr.config")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AnprC001ServiceConfig extends BasePdndServiceConfig {
}
