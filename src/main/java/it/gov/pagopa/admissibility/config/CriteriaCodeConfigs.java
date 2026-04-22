package it.gov.pagopa.admissibility.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties(prefix = "app.criteria-code-configs")
@Data
public class CriteriaCodeConfigs {

    /**
     * Map keyed by criteria code (e.g. ISEE, RESIDENCE, BIRTHDATE)
     */
    private Map<String, CriteriaConfig> configs = new HashMap<>();

    @Data
    public static class CriteriaConfig {

        /**
         * Authority owning the criterion (INPS, AGID, ANPR, ...)
         */
        private String authority;

        /**
         * Human readable authority label
         */
        private String authorityLabel;

        /**
         * Field of OnboardingDTO populated by the criterion
         * (isee, residence, birthDate, family, ...)
         */
        private String onboardingField;

        /**
         * PDND client to be used for this criterion
         * (e.g. c001, c021)
         */
        private String pdndClient;
    }
}