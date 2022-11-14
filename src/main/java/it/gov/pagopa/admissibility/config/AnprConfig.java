package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.dto.agid.AgidJwtToken;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("app.anpr.digest")
public class AnprConfig extends AgidJwtToken {
}
