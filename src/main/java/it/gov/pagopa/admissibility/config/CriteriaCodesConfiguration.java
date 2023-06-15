package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class CriteriaCodesConfiguration {
    /** mocked criteria codes */
    private Map<String, CriteriaCodeConfig> criteriaCodeConfigs;

    @PostConstruct
    public void alignKeys(){
        criteriaCodeConfigs.forEach((key, value) -> value.setCode(key));
    }
}
