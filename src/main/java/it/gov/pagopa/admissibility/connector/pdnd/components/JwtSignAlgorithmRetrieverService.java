package it.gov.pagopa.admissibility.connector.pdnd.components;

import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;

/** To retrieve the {@link Algorithm} used when signing PDND's JWT */
public interface JwtSignAlgorithmRetrieverService {
    Algorithm retrieve(PdndInitiativeConfig pdndInitiativeConfig);
}
