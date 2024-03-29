package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import it.gov.pagopa.common.mongo.MongoTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@MongoTest
class CustomSequenceGeneratorOpsRepositoryImplTest {
    @Autowired
    protected CustomSequenceGeneratorRepository customSequenceGeneratorRepository;

    private final String TEST_ID = "testSequenceId";

    @AfterEach
    void cleanData(){
        customSequenceGeneratorRepository.deleteById(TEST_ID).block();
    }


    @Test
    void getSequenceAlreadyExits(){
        // Given
        Long initialSequence = 5L;
        CustomSequenceGenerator customSequenceGenerator =  CustomSequenceGenerator.builder()
                .sequenceId(TEST_ID)
                .value(initialSequence)
                .build();
        customSequenceGeneratorRepository.save(customSequenceGenerator).block();

        // When
        Long sequenceResult = customSequenceGeneratorRepository.nextValue(TEST_ID).block();

        // Then
        System.out.println(sequenceResult);
        Assertions.assertNotNull(sequenceResult);
        Assertions.assertNotEquals(initialSequence, sequenceResult);
        Assertions.assertEquals(6L, sequenceResult);
    }

    @Test
    void getSequenceNotExits(){
        // When
        Long sequenceResult1 = customSequenceGeneratorRepository.nextValue(TEST_ID).block();
        Long sequenceResult2 = customSequenceGeneratorRepository.nextValue(TEST_ID).block();

        // Then
        Assertions.assertNotNull(sequenceResult1);
        Assertions.assertEquals(1L, sequenceResult1);

        Assertions.assertNotNull(sequenceResult2);
        Assertions.assertEquals(2L, sequenceResult2);
    }

}