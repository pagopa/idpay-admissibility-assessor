package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryExt;

/**
 * it will handle the persistence of {@link InitiativeCounters} entity*/
public interface InitiativeCountersRepository extends ReactiveMongoRepositoryExt<InitiativeCounters, String>, InitiativeCountersReservationOpsRepository {
}
