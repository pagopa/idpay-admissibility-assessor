package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InitiativeCountersPreallocationsOpsRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private InitiativeCountersPreallocationsOpsRepositoryImpl repository;

    private static final String TEST_ID = "testId";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteByIdReturningResult_Success() {
        DeleteResult deleteResult = DeleteResult.acknowledged(1);
        when(mongoTemplate.remove(any(), any(Class.class)))
                .thenReturn(Mono.just(deleteResult));

        StepVerifier.create(repository.deleteByIdReturningResult(TEST_ID))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testDeleteByIdReturningResult_Failure() {
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(mongoTemplate.remove(any(), any(Class.class)))
                .thenReturn(Mono.just(deleteResult));

        StepVerifier.create(repository.deleteByIdReturningResult(TEST_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testUpdatePreallocatedAmount_Success() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), any(Class.class)))
                .thenReturn(Mono.just(updateResult));

        StepVerifier.create(repository.updatePreallocatedAmount(TEST_ID, 1000L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testUpdatePreallocatedAmount_Failure() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(mongoTemplate.updateFirst(any(), any(), any(Class.class)))
                .thenReturn(Mono.just(updateResult));

        StepVerifier.create(repository.updatePreallocatedAmount(TEST_ID, 1000L))
                .expectNext(false)
                .verifyComplete();
    }
}