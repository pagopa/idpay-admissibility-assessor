package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OnboardingFamiliesRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private OnboardingFamiliesRepositoryExtImpl repository;

    private static final String INITIATIVE_ID = "initiative1";
    private Family testFamily;
    private OnboardingFamilyEvaluationStatus status;
    private UpdateResult updateResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testFamily = new Family();
        testFamily.setFamilyId("family1");

        status = OnboardingFamilyEvaluationStatus.ONBOARDING_OK;

        updateResult = UpdateResult.acknowledged(1L, 1L, null);
    }

    @Test
    void testUpdateOnboardingFamilyOutcome() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), any(Class.class)))
                .thenReturn(Mono.just(updateResult));

        StepVerifier.create(repository.updateOnboardingFamilyOutcome(
                        testFamily,
                        INITIATIVE_ID,
                        status,
                        Collections.emptyList()))
                .expectNext(updateResult)
                .verifyComplete();
    }

    @Test
    void testFindByInitiativeIdWithBatch() {
        OnboardingFamilies mockFamily = new OnboardingFamilies();
        mockFamily.setId(OnboardingFamilies.buildId(testFamily, INITIATIVE_ID));

        when(mongoTemplate.find(any(Query.class), any(Class.class)))
                .thenReturn(Flux.just(mockFamily));

        StepVerifier.create(repository.findByInitiativeIdWithBatch(INITIATIVE_ID, 10))
                .expectNext(mockFamily)
                .verifyComplete();
    }
}