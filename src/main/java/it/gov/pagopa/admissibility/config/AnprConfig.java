package it.gov.pagopa.admissibility.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnprConfig {

    @Configuration
    @ConfigurationProperties("app.anpr.digest.header")
    @Data
    public class TokenHeader{
        private String alg;
        private String kid;
        private String typ;
    }

    @Configuration
    @ConfigurationProperties("app.anpr.digest.header")
    @Data
    public class TokenPayload{
        private String iss;
        private String sub;
        private String aud;
        private long jti; //#TODO UUID.randomUUID().toString();
        private long iat; //TODO long long nowSeconds = System.currentTimeMillis() / 1000L;
        private long exp; //TODO long long expireSeconds = nowSeconds + 5000L;
        private String purposeID; // clientId
    }
}
