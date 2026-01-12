package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

public interface AnprC021RestClient {
    Mono<?> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig);
}