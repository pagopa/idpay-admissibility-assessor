package it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

public interface AnprC001RestClient {
    Mono<RispostaE002OKDTO> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig);
}