package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import reactor.core.publisher.Mono;

public interface InitiativeStatusService {

    Mono<InitiativeStatusDTO> getInitiativeStatusAndBudgetAvailable(String initiativeId);
}
