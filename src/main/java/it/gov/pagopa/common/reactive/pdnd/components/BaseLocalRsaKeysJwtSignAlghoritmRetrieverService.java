package it.gov.pagopa.common.reactive.pdnd.components;

import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.crypto.utils.JwtUtils;

public class BaseLocalRsaKeysJwtSignAlghoritmRetrieverService implements JwtSignAlgorithmRetrieverService {

    private final Algorithm algorithm;

    public BaseLocalRsaKeysJwtSignAlghoritmRetrieverService(
            String jwtSignPublicKey,
            String jwtSignPrivateKey
    ){
        this.algorithm = JwtUtils.buildLocalRsaKeysJwtSignAlgorithm(jwtSignPublicKey, jwtSignPrivateKey);
    }

    @Override
    public Algorithm retrieve(PdndInitiativeConfig pdndInitiativeConfig) {
        return algorithm;
    }
}
