package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import reactor.core.publisher.Mono;

public interface InitiativeCountersReservationOpsRepository {
    Mono<InitiativeCounters> deallocatedPartialBudget(String initiativeId, long deallocatedBudget);
    Mono<InitiativeCounters> deallocateBudget(String initiativeId, long deallocatedBudget);
}
