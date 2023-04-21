package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class OnboardingFamiliesRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private OnboardingFamiliesRepository repository;

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
    void testFindByMemberIdsIn() {
        OnboardingFamilies f1 = storeTestFamily(OnboardingFamilies.builder()
                .id("TESTFAMILYID")
                .memberIds(Set.of("ID1", "ID2"))
                .build());

        OnboardingFamilies f2 = storeTestFamily(OnboardingFamilies.builder()
                .id("TESTFAMILYID2")
                .memberIds(Set.of("ID2", "ID3"))
                .build());

        List<OnboardingFamilies> result = repository.findByMemberIdsIn("ID1").collectList().block();
        Assertions.assertEquals(List.of(f1), result);

        result = repository.findByMemberIdsIn("ID2").collectList().block();
        Assertions.assertEquals(List.of(f1, f2), result);

        result = repository.findByMemberIdsIn("ID4").collectList().block();
        Assertions.assertEquals(Collections.emptyList(), result);
    }
}
