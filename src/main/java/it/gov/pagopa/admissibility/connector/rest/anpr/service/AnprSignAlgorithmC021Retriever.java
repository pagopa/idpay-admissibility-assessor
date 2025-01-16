package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.common.reactive.pdnd.components.BaseLocalRsaKeysJwtSignAlghoritmRetrieverService;
import it.gov.pagopa.common.reactive.pdnd.components.JwtSignAlgorithmRetrieverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnprSignAlgorithmC021Retriever extends BaseLocalRsaKeysJwtSignAlghoritmRetrieverService implements JwtSignAlgorithmRetrieverService {

    public AnprSignAlgorithmC021Retriever(
            @Value("${app.anpr.pagopa-pdnd-configuration.c021.pub}") String jwtSignPublicKey,
            @Value("${app.anpr.pagopa-pdnd-configuration.c021.key}") String jwtSignPrivateKey
    ) {
        super(jwtSignPublicKey, jwtSignPrivateKey);
    }

}
