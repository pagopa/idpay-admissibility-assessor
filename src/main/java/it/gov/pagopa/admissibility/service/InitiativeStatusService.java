package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import reactor.core.publisher.Flux;

public interface InitiativeStatusService {

    Flux<InitiativeStatusDTO> getInitiativeStatusAndBudgetAvailable(String initiativeId);
}
