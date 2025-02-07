package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.AnprInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
/**
 * it will handle the persistence of {@link AnprInfo} entity*/
public interface AnprInfoRepository extends ReactiveMongoRepository<AnprInfo, String>, AnprInfoRepositoryExt{



}
