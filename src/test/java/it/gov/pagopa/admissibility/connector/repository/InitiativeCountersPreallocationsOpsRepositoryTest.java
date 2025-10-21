package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCountersPreallocations;
import it.gov.pagopa.common.mongo.MongoTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static it.gov.pagopa.admissibility.utils.Utils.computePreallocationId;

@MongoTest
class InitiativeCountersPreallocationsOpsRepositoryTest {

    @Autowired
    protected InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    @Autowired
    private InitiativeCountersPreallocationsOpsRepositoryImpl initiativeCountersPreallocationsOpsRepositoryImpl;


    @Test
    void deleteByIdReturningResult() {
        String userId = "USERID";
        String initiativeId = "INITIATIVEID";
        String preallocationId = computePreallocationId(userId, initiativeId);
        InitiativeCountersPreallocations preallocations = InitiativeCountersPreallocations.builder()
                .id(preallocationId)
                .userId(userId)
                .initiativeId(initiativeId)
                .build();

        initiativeCountersPreallocationsRepository.save(preallocations).block();

        Boolean result = initiativeCountersPreallocationsOpsRepositoryImpl.deleteByIdReturningResult(preallocationId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(true, result);

        InitiativeCountersPreallocations after = initiativeCountersPreallocationsRepository.findById(preallocationId).block();
        Assertions.assertNull(after);
    }

    @Test
    void deleteByIdReturningResult_false() {
        String userId = "USERIDNOTSAVED";
        String initiativeId = "INITIATIVEID";
        String preallocationId = computePreallocationId(userId, initiativeId);

        Boolean result = initiativeCountersPreallocationsOpsRepositoryImpl.deleteByIdReturningResult(preallocationId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(false, result);
    }
}