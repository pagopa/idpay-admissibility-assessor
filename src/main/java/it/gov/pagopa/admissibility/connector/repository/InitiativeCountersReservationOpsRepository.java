package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import reactor.core.publisher.Mono;

public interface InitiativeCountersReservationOpsRepository {
    Mono<InitiativeCounters> reserveBudget(String initiativeId, Long reservationCents);
}
