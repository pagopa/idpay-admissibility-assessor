package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCountersPreallocations;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface InitiativeCountersPreallocationsRepository extends ReactiveMongoRepository<InitiativeCountersPreallocations, String>, InitiativeCountersPreallocationsOpsRepository {
}
