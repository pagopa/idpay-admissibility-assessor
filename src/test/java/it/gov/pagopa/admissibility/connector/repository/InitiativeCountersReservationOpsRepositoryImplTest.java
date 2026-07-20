package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InitiativeCountersReservationOpsRepositoryImplTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private InitiativeCountersReservationOpsRepositoryImpl repository;

    private static final String INITIATIVE_ID = "initiative1";

    private InitiativeCounters mockCounter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockCounter = new InitiativeCounters();
        mockCounter.setId(INITIATIVE_ID);
        mockCounter.setResidualInitiativeBudgetCents(1000L);
        mockCounter.setReservedInitiativeBudgetCents(0L);
        mockCounter.setOnboarded(0L);
        mockCounter.setUpdateDate(LocalDateTime.now());
    }

    @Test
    void testReserveBudget() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), any(Class.class)))
                .thenReturn(Mono.just(mockCounter));

        StepVerifier.create(repository.reserveBudget(INITIATIVE_ID, 500L))
                .expectNext(mockCounter)
                .verifyComplete();
    }

    @Test
    void testDeallocatedPartialBudget() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), any(Class.class)))
                .thenReturn(Mono.just(mockCounter));

        StepVerifier.create(repository.deallocatedPartialBudget(INITIATIVE_ID, 200L))
                .expectNext(mockCounter)
                .verifyComplete();
    }

    @Test
    void testDeallocateBudget() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), any(Class.class)))
                .thenReturn(Mono.just(mockCounter));

        StepVerifier.create(repository.deallocateBudget(INITIATIVE_ID, 300L))
                .expectNext(mockCounter)
                .verifyComplete();
    }
}