package it.gov.pagopa.admissibility.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.criteria-code-configs")
@Data
public class CriteriaCodeConfigs {
    private Map<String, CriteriaConfig> configs = new HashMap<>();
    @Data
    public static class CriteriaConfig {
        private String authority;
        private String authorityLabel;
        private String onboardingField;
    }
}