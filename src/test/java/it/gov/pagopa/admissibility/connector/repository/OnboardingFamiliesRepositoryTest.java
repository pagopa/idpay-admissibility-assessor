package it.gov.pagopa.admissibility.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.common.mongo.MongoTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@MongoTest
class OnboardingFamiliesRepositoryTest{

    @Autowired
    private OnboardingFamiliesRepository repository;
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-03T10:00:00Z"), ZoneOffset.UTC);

    private final List<OnboardingFamilies> testData = new ArrayList<>();

    @AfterEach
    void clearTestData() {
        repository.deleteAll(testData).block();
    }

    private OnboardingFamilies storeTestFamily(OnboardingFamilies e) {
        OnboardingFamilies out = repository.save(e).block();
        testData.add(out);
        return out;
    }

    @Test
    void testFindByMemberIdsInAndInitiativeId() {
        String initiativeId = "INITIATIVEID";

        OnboardingFamilies f1 = storeTestFamily(new OnboardingFamilies(new Family("FAMILYID", Set.of("ID1", "ID2")), initiativeId));
        storeTestFamily(f1.toBuilder().initiativeId("INITIATIVEID2").build());

        OnboardingFamilies f2 = storeTestFamily(new OnboardingFamilies(new Family("FAMILYID2", Set.of("ID2", "ID3")), initiativeId));
        storeTestFamily(f2.toBuilder().initiativeId("INITIATIVEID2").build());

        //checking if builder custom implementation is successfully handling ids
        assertStoredData();
        Assertions.assertEquals(4, testData.stream().map(OnboardingFamilies::getId).count());

        List<OnboardingFamilies> result = repository.findByMemberIdsInAndInitiativeId("ID1", initiativeId).collectList().block();
        Assertions.assertEquals(List.of(f1), result);

        result = repository.findByMemberIdsInAndInitiativeId("ID2", initiativeId).collectList().block();
        Assertions.assertEquals(List.of(f1, f2), result);

        result = repository.findByMemberIdsInAndInitiativeId("ID4", initiativeId).collectList().block();
        Assertions.assertEquals(Collections.emptyList(), result);
    }

    private void assertStoredData() {
        testData.forEach(t -> Assertions.assertEquals(t, repository.findById(t.getId()).block()));
    }

    @Test
    void testCreateIfNotExists(){
        // Given
        Instant beforeCreate = Instant.now(clock);

        Family f1 = new Family("FAMILYID", Set.of("ID1", "ID2"));
        OnboardingFamilies expectedResult = new OnboardingFamilies(f1, "INITIATIVEID");
        expectedResult.setStatus(OnboardingFamilyEvaluationStatus.IN_PROGRESS);
        expectedResult.setCreateBy("ID1");
        testData.add(expectedResult);

        // When not exists
        OnboardingFamilies result = repository.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(f1, expectedResult.getInitiativeId(), expectedResult.getCreateBy(),clock).block();

        // Then after create
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getCreateDate());
        Assertions.assertFalse(result.getCreateDate().isBefore(beforeCreate));
        result.setCreateDate(result.getCreateDate().truncatedTo(ChronoUnit.MILLIS));
        result.setUpdateDate(result.getUpdateDate().truncatedTo(ChronoUnit.MILLIS));
        expectedResult.setCreateDate(result.getCreateDate());
        expectedResult.setUpdateDate(result.getUpdateDate());
        Assertions.assertEquals(expectedResult, result);

        // When exists
        OnboardingFamilies resultWhenAlreadyExists = repository.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(f1, expectedResult.getInitiativeId(), expectedResult.getCreateBy(),clock).block();

        // Then afterDeleteResult
        Assertions.assertNull(resultWhenAlreadyExists);

        Assertions.assertEquals(result, repository.findById("FAMILYID_INITIATIVEID").block());
    }

    @Test
    void testUpdateOnboardingFamilyOutcome(){
        // Given
        String initiativeId = "INITIATIVEID";

        Family f1 = new Family("FAMILYID", Set.of("ID1", "ID2"));
        OnboardingFamilies of1 = storeTestFamily(new OnboardingFamilies(f1, initiativeId));
        OnboardingFamilies of2 = storeTestFamily(new OnboardingFamilies(new Family("FAMILYID2", Set.of("ID2", "ID3")), initiativeId));

        OnboardingFamilyEvaluationStatus updatedStatus = OnboardingFamilyEvaluationStatus.ONBOARDING_OK;
        List<OnboardingRejectionReason> updatedOnboardingRejectionReasons = List.of(new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR, "DUMMY", "AUTH", "AUTHLABEL", "DETAILS"));

        // When
        UpdateResult result = repository.updateOnboardingFamilyOutcome(f1, initiativeId, updatedStatus, updatedOnboardingRejectionReasons).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(1L, result.getModifiedCount());
        Assertions.assertSame(1L, result.getMatchedCount());

        of1.setStatus(updatedStatus);
        of1.setOnboardingRejectionReasons(updatedOnboardingRejectionReasons);
        OnboardingFamilies expected1 = repository.findById(of1.getId()).block();
        OnboardingFamilies expected2 = repository.findById(of2.getId()).block();
        assert expected1 != null;
        of1.setUpdateDate(expected1.getUpdateDate());
        assert expected2 != null;
        of2.setUpdateDate(expected2.getUpdateDate());
        Assertions.assertEquals(of1, expected1);
        Assertions.assertEquals(of2, expected2);
    }
}
