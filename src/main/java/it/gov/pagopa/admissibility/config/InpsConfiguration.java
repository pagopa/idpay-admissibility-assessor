package it.gov.pagopa.admissibility.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class InpsConfiguration {
    private Inps inps;

    @Getter
    @Setter
    public static class Inps {
        private IseeConsultation iseeConsultation;
        private Header header;
        private Secure secure;
    }
    @Getter
    @Setter
    public static class IseeConsultation {
        private String baseUrl;
        private Config config;

        private boolean mockEnabled;
        private String mockBaseUrl;
        private String realBaseUrl;

        @PostConstruct
        public void init() {
            this.baseUrl = mockEnabled ? mockBaseUrl : realBaseUrl;
            log.info("[INPS_ISEE_CONFIGURATION] Using {} base URL: {}", mockEnabled ? "MOCK" : "REAL", baseUrl);
        }
    }

    @Getter
    @Setter
    public static class Config {
        private Integer connectionTimeout;
        private Integer requestTimeout;
    }

    @Getter
    @Setter
    public static class Header {
        private String officeCode;
        private String userId;
    }

    @Getter
    @Setter
    public static class Secure {
        private String cert;
        private String key;
    }

    public String getBaseUrlForInps() {
        if (inps != null && inps.getIseeConsultation() != null) {
            IseeConsultation ic = inps.getIseeConsultation();
            if (ic.isMockEnabled()) {
                return ic.getMockBaseUrl();
            } else {
                return ic.getRealBaseUrl();
            }
        }
        return null;
    }

    public Integer getConnectionTimeoutForInps() {
        if (inps != null && inps.getIseeConsultation() != null) {
            Config config = inps.getIseeConsultation().getConfig();
            if(config != null) {
                return config.getConnectionTimeout();
            }
        }
        return null;
    }
    public Integer getRequestTimeoutForInps() {
        if (inps != null && inps.getIseeConsultation() != null) {
            Config config = inps.getIseeConsultation().getConfig();
            if(config != null) {
                return config.getRequestTimeout();
            }
        }
        return null;
    }

    public String getOfficeCodeForInps() {
        if (inps != null && inps.getHeader() != null) {
            return inps.getHeader().getOfficeCode();
        }
        return null;
    }

    public String getUserIdForInps() {
        if (inps != null && inps.getHeader() != null) {
            return inps.getHeader().getUserId();
        }
        return null;
    }

    public String getCertForInps() {
        if (inps != null && inps.getSecure() != null) {
            return inps.getSecure().getCert();
        }
        return null;
    }

    public String getKeyForInps() {
        if (inps != null && inps.getSecure() != null) {
            return inps.getSecure().getKey();
        }
        return null;
    }
}
