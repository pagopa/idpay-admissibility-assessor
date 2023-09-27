package it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.components.BaseLocalRsaKeysJwtSignAlghoritmRetrieverService;
import it.gov.pagopa.admissibility.connector.pdnd.components.JwtSignAlgorithmRetrieverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnprSignAlgorithmRetriever extends BaseLocalRsaKeysJwtSignAlghoritmRetrieverService implements JwtSignAlgorithmRetrieverService {

    public AnprSignAlgorithmRetriever(
            @Value("${app.anpr.web-client.jwt-agid.pub}") String jwtSignPublicKey,
            @Value("${app.anpr.web-client.jwt-agid.key}") String jwtSignPrivateKey
    ) {
        super(jwtSignPublicKey, jwtSignPrivateKey);
    }

}
