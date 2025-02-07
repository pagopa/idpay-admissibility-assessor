package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.AnprInfo;
import reactor.core.publisher.Flux;

/**
 * it will handle the persistence of {@link AnprInfo} entity*/
public interface AnprInfoRepositoryExt{
    Flux<AnprInfo> findByInitiativeIdWithBatch(String initiativeId, int batchSize);
}
