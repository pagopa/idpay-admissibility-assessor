package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
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
}
