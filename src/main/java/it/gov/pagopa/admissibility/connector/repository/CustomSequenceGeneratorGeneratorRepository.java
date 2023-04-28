package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * it will handle the persistence of {@link CustomSequenceGenerator} entity*/
public interface CustomSequenceGeneratorGeneratorRepository extends ReactiveMongoRepository<CustomSequenceGenerator, String>, CustomSequenceGeneratorOpsRepository {
}
