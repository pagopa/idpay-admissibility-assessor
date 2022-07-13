package it.gov.pagopa.admissibility.rest.initiative;

import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;

/**
 * This component will communicate with the initiative microservice
 * */
public interface InitiativeRestService {
    InitiativeDTO findById(String initiativeId);
}
