package it.gov.pagopa.admissibility.rest.initiative;

import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import reactor.core.publisher.Mono;

/**
 * This component will communicate with the initiative microservice
 * */
public interface InitiativeRestService {
    Mono<InitiativeDTO> findById(String initiativeId);
}
