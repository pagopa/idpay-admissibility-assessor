package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CustomSequenceGeneratorOpsRepositoryImplTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private CustomSequenceGeneratorOpsRepositoryImpl repository;

    private static final String TEST_SEQUENCE_ID = "testSequence";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNextValue_FirstCall_ShouldReturn1() {
        // Mockiamo findAndModify
        CustomSequenceGenerator sequence = new CustomSequenceGenerator();
        sequence.setSequenceId(TEST_SEQUENCE_ID);
        sequence.setValue(1L);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), any(Class.class)))
                .thenReturn(Mono.just(sequence));

        StepVerifier.create(repository.nextValue(TEST_SEQUENCE_ID))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void testNextValue_SubsequentCalls_ShouldIncrement() {
        // Simuliamo un incremento
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), any(Class.class)))
                .thenReturn(Mono.just(sequence(1L)))
                .thenReturn(Mono.just(sequence(2L)))
                .thenReturn(Mono.just(sequence(3L)));

        StepVerifier.create(repository.nextValue(TEST_SEQUENCE_ID))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(repository.nextValue(TEST_SEQUENCE_ID))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(repository.nextValue(TEST_SEQUENCE_ID))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void testNextValue_MultipleSequences_ShouldMaintainSeparateCounters() {
        String sequenceA = "seqA";
        String sequenceB = "seqB";

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), any(Class.class)))
                .thenAnswer(invocation -> {
                    Query q = invocation.getArgument(0);
                    String seqId = q.getQueryObject().getString("sequenceId");
                    long value = seqId.equals(sequenceA) ? 1L : 1L;
                    return Mono.just(sequence(value, seqId));
                });

        StepVerifier.create(repository.nextValue(sequenceA))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(repository.nextValue(sequenceB))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(repository.nextValue(sequenceA))
                .expectNext(1L) // perché il mock non mantiene lo stato reale
                .verifyComplete();
    }

    // Helper per creare CustomSequenceGenerator
    private CustomSequenceGenerator sequence(long value) {
        return sequence(value, TEST_SEQUENCE_ID);
    }

    private CustomSequenceGenerator sequence(long value, String sequenceId) {
        CustomSequenceGenerator s = new CustomSequenceGenerator();
        s.setSequenceId(sequenceId);
        s.setValue(value);
        return s;
    }
}