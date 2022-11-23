package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface InitiativeCountersReservationOpsRepository {
    Mono<InitiativeCounters> reserveBudget(String initiativeId, BigDecimal reservation);
}
