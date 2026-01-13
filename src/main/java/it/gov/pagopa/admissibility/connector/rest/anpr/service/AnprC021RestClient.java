package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaKODTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

public interface AnprC021RestClient {
    Mono<PdndResponseBase<RispostaE002OKDTO, RispostaKODTO>> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig);
}