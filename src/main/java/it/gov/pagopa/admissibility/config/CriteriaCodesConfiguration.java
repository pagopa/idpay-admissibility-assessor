package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.criteria-code")
@Data
public class CriteriaCodesConfiguration {

    private Map<String, CriteriaCodeConfig> configs = new HashMap<>();

    @PostConstruct
    public void alignKeys() {
        configs.forEach((key, value) -> value.setCode(key));
    }
}