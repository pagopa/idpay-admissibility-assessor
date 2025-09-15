package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class Onboarding2OnboardingDroolsMapperTest {

    @Test
    void onboarding2OnboardingDroolsFilledTest() {

        // GIVEN
        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                new Family(),
                false,
                "SERVICE",
                Boolean.TRUE,
                Boolean.TRUE
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
        Assertions.assertEquals(localDateTimeMock1, result.getTcAcceptTimestamp());
        Assertions.assertEquals(localDateTimeMock1, result.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(new BigDecimal(100), result.getIsee());
        Assertions.assertNotNull(result.getOnboardingRejectionReasons());
        Assertions.assertSame(objectMock1.getResidence(), result.getResidence());
        Assertions.assertSame(objectMock1.getBirthDate(), result.getBirthDate());

        TestUtils.checkNotNullFields(result,"serviceId", "verifyIsee", "underThreshold");
    }
}
