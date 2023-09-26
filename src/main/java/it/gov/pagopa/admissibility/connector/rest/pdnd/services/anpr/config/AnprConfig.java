package it.gov.pagopa.admissibility.connector.rest.pdnd.services.anpr.config;

import it.gov.pagopa.admissibility.connector.rest.pdnd.config.BaseAgidConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.anpr.common.config")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AnprConfig extends BaseAgidConfig {
    private long authExpirationSeconds;
}
