package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * it will handle the persistence of {@link InitiativeCounters} entity*/
public interface InitiativeCountersRepository extends ReactiveMongoRepository<InitiativeCounters, String>, InitiativeCountersReservationOpsRepository {
}
