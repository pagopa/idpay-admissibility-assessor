package it.gov.pagopa.admissibility.rest.agid;

import it.gov.pagopa.admissibility.config.AnprConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnprJwtSignature extends AgidJwtSignature {
    public AnprJwtSignature(AnprConfig anprConfig,
                            @Value("${app.anpr.web-client.jwt-agid.cert}") String cert,
                            @Value("${app.anpr.web-client.jwt-agid.key}") String key,
                            @Value("${app.anpr.web-client.jwt-agid.pub}") String pub) {
        super(anprConfig,cert,key,pub);
    }
}