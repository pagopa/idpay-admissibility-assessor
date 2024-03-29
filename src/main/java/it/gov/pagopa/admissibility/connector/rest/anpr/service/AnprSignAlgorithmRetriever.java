package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.common.reactive.pdnd.components.BaseLocalRsaKeysJwtSignAlghoritmRetrieverService;
import it.gov.pagopa.common.reactive.pdnd.components.JwtSignAlgorithmRetrieverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnprSignAlgorithmRetriever extends BaseLocalRsaKeysJwtSignAlghoritmRetrieverService implements JwtSignAlgorithmRetrieverService {

    public AnprSignAlgorithmRetriever(
            @Value("${app.anpr.pagopa-pdnd-configuration.pub}") String jwtSignPublicKey,
            @Value("${app.anpr.pagopa-pdnd-configuration.key}") String jwtSignPrivateKey
    ) {
        super(jwtSignPublicKey, jwtSignPrivateKey);
    }

}
