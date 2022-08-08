package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import reactor.core.publisher.Mono;

/** It will initialize the counters */
public interface InitInitiativeCounterService {
    Mono<InitiativeCounters> initCounters(InitiativeConfig initiative);
}
