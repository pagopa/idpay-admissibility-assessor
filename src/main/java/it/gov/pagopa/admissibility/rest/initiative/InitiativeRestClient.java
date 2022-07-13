package it.gov.pagopa.admissibility.rest.initiative;

import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;

/**The client through which communicate towards the initiative microservice*/
public interface InitiativeRestClient { //TODO
    InitiativeDTO findById(String initiativeId);
}
