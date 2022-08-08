package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * it will handle the persistence of {@link InitiativeCounters} entity*/
public interface InitiativeCountersRepository extends ReactiveMongoRepository<InitiativeCounters, String> {
}
