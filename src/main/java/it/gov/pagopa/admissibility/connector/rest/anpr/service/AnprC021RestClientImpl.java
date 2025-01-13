package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import reactor.core.publisher.Mono;

public class AnprC021RestClientImpl implements AnprC021RestClient{
    @Override
    public Mono<RispostaE002OKDTO> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig) {
        return null;
    }
}
