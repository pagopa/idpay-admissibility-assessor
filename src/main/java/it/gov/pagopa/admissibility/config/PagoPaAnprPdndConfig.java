package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.anpr")
public class PagoPaAnprPdndConfig {
    private Map<String, PdndInitiativeConfig> pagopaPdndConfiguration;

}