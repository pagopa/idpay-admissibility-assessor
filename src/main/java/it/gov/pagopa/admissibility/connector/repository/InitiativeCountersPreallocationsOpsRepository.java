package it.gov.pagopa.admissibility.connector.repository;

import reactor.core.publisher.Mono;

public interface InitiativeCountersPreallocationsOpsRepository {
    Mono<Boolean> deleteByIdReturningResult(String id);
}
