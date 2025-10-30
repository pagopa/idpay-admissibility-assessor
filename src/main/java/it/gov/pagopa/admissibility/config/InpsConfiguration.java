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
    private InpsMock inpsMock;

    @Getter
    @Setter
    public static class Inps {
        private IseeConsultation iseeConsultation;
        private Header header;
        private Secure secure;
    }

    @Getter
    @Setter
    public static class InpsMock {
        private boolean enabledIsee;
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class IseeConsultation {
        private String baseUrl;
        private Config config;

        private String realBaseUrl;
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
        if (inpsMock.isEnabledIsee()){
            return inpsMock.getBaseUrl();
        }
        if (inps != null && inps.getIseeConsultation() != null) {
            return inps.getIseeConsultation().getBaseUrl();
        }
        return null;
    }

    public Integer getConnectionTimeoutForInps() {
        if (inps != null && inps.getIseeConsultation() != null && inps.getIseeConsultation().getConfig() != null) {
            return inps.getIseeConsultation().getConfig().getConnectionTimeout();
        }
        return null;
    }

    public Integer getRequestTimeoutForInps() {
        if (inps != null && inps.getIseeConsultation() != null && inps.getIseeConsultation().getConfig() != null) {
            return inps.getIseeConsultation().getConfig().getRequestTimeout();
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
