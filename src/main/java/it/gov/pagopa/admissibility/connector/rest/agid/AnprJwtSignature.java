package it.gov.pagopa.admissibility.connector.rest.agid;

import it.gov.pagopa.admissibility.config.AnprConfig;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnprJwtSignature extends AgidJwtSignature {
    @Getter
    private final String trackingEvidence;

    public AnprJwtSignature(AnprConfig anprConfig,
                            @Value("${app.anpr.web-client.jwt-agid.tracking-evidence}") String trackingEvidence,
                            @Value("${app.anpr.web-client.jwt-agid.aud}") String aud,
                            @Value("${app.anpr.web-client.jwt-agid.kid}") String kid,
                            @Value("${app.anpr.web-client.jwt-agid.key}") String key,
                            @Value("${app.anpr.web-client.jwt-agid.pub}") String pub) {
        super(anprConfig,aud,kid,key,pub);
        this.trackingEvidence=trackingEvidence;
    }
}