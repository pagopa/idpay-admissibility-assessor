package it.gov.pagopa.common.reactive.pdnd.config;

import lombok.Data;

@Data
public abstract class BasePdndServiceProviderConfig {
    private String baseUrl;
    private long authExpirationSeconds;
    private HttpsConfig httpsConfig;
    private AgidConfig agidConfig;

    @Data
    public static class AgidConfig {
        private String env;
        private String userId;
    }

    @Data
    public static class HttpsConfig {
        private boolean enabled;
        private String cert;
        private String key;
        private boolean mutualAuthEnabled;
        private String trustCertificatesCollection;
    }
}
