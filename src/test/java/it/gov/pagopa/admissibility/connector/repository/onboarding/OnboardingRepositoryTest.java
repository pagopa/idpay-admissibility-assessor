package it.gov.pagopa.admissibility.connector.repository.onboarding;

import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import it.gov.pagopa.admissibility.model.onboarding.OnboardingFamilyInfo;
import it.gov.pagopa.common.mongo.MongoTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@MongoTest
class OnboardingRepositoryTest {
    private static final String INITIATIVE = "INITIATIVE_ID";

    @Autowired
    private OnboardingRepository onboardingRepository;

    private final List<Onboarding> testData = new ArrayList<>();

    @AfterEach
    void clearTestData() {
        onboardingRepository.deleteAll(testData).block();
        testData.clear();
    }

    @Test
    void findByInitiativeIdAndUserIdInAndStatus() {
        String userId = "USER_ID";
        String familyId = "FAMILY_ID";
        Onboarding o1 = new Onboarding(INITIATIVE, userId);
        o1.setFamilyId(familyId);
        o1.setStatus(OnboardingEvaluationStatus.ONBOARDING_OK.name());

        String userId2 = "USER_ID_2";
        Onboarding o2 = new Onboarding(INITIATIVE, userId2);
        o2.setFamilyId(familyId);
        o2.setStatus("JOINED");

        List<Onboarding> onboardings = List.of(o1, o2);
        onboardingRepository.saveAll(onboardings).blockLast();
        testData.addAll(onboardings);

        Set<String> onboardingIds = new HashSet<>(Set.of(userId, userId2)).stream().map(u -> Onboarding.buildId(INITIATIVE, u)).collect(Collectors.toSet());
        List<OnboardingFamilyInfo> result = onboardingRepository.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name())
                .collectList()
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(userId, result.getFirst().getUserId());
        Assertions.assertEquals(familyId, result.getFirst().getFamilyId());
    }

    @Test
    void findByInitiativeIdAndUserIdInAndStatus_NotFamilyOnboarded() {
        String userId = "USER_ID";
        String familyId = "FAMILY_ID";
        Onboarding o1 = new Onboarding(INITIATIVE, userId);
        o1.setFamilyId(familyId);
        o1.setStatus(OnboardingEvaluationStatus.ONBOARDING_OK.name());

        List<Onboarding> onboardings = List.of(o1);
        onboardingRepository.saveAll(onboardings).blockLast();
        testData.addAll(onboardings);

        Set<String> onboardingIds = new HashSet<>(Set.of(userId)).stream().map(u -> Onboarding.buildId("INITIATIVE_2", u)).collect(Collectors.toSet());
        List<OnboardingFamilyInfo> result = onboardingRepository.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name())
                .collectList()
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }
}