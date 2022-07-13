package it.gov.pagopa.admissibility.dto.onboarding.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

class Onboarding2OnboardingDroolsMapperTest {

    @Test
    void onboarding2OnboardingDroolsFilledTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationListMock1 = new HashMap<>();
        selfDeclarationListMock1.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListMock1,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100)
        );

        Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = new Onboarding2OnboardingDroolsMapper();

        // WHEN
        OnboardingDroolsDTO result = onboarding2OnboardingDroolsMapper.apply(objectMock1);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertEquals("1", result.getInitiativeId());
        Assertions.assertTrue(result.isTc());
        Assertions.assertEquals("OK", result.getStatus());
        Assertions.assertEquals(true, result.getPdndAccept());
        Assertions.assertEquals(selfDeclarationListMock1, result.getSelfDeclarationList());
        Assertions.assertEquals(localDateTimeMock1, result.getTcAcceptTimestamp());
        Assertions.assertEquals(localDateTimeMock1, result.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(new BigDecimal(100), result.getIsee());
        Assertions.assertNotNull(result.getOnboardingRejectionReasons());
    }

    @Test
    void onboarding2OnboardingDroolsNullFieldTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationListMock1 = new HashMap<>();
        selfDeclarationListMock1.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                "1",
                true,
                null,
                true,
                selfDeclarationListMock1,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100)
        );

        Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = new Onboarding2OnboardingDroolsMapper();

        // WHEN
        OnboardingDroolsDTO result = onboarding2OnboardingDroolsMapper.apply(objectMock1);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertEquals("1", result.getInitiativeId());
        Assertions.assertTrue(result.isTc());
        Assertions.assertNull(result.getStatus());
        Assertions.assertEquals(true, result.getPdndAccept());
        Assertions.assertEquals(selfDeclarationListMock1, result.getSelfDeclarationList());
        Assertions.assertEquals(localDateTimeMock1, result.getTcAcceptTimestamp());
        Assertions.assertEquals(localDateTimeMock1, result.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(new BigDecimal(100), result.getIsee());
        Assertions.assertNotNull(result.getOnboardingRejectionReasons());
    }
}
