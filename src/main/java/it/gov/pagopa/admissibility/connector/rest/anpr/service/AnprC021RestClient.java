package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

public interface AnprC021RestClient {
    Mono<RispostaE002OKDTO> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig);
}