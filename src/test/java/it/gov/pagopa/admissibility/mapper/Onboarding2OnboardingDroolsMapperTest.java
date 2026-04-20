package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.VerifyDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class Onboarding2OnboardingDroolsMapperTest {

    @Test
    void onboarding2OnboardingDroolsFilledTest() {

        // GIVEN
        LocalDateTime now = LocalDateTime.now();

        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId("1")
                .tc(true)
                .status("OK")
                .pdndAccept(true)
                .tcAcceptTimestamp(now)
                .criteriaConsensusTimestamp(now)
                .isee(new BigDecimal(100))
                .residence(new Residence())
                .birthDate(new BirthDate())
                .family(new Family())
                .serviceId("SERVICE")
                .userMail("USERMAIL")
                .channel("CHANNEL")
                .name("NAME")
                .surname("SURNAME")
                .verifies(List.of(
                        new VerifyDTO(
                                "ISEE",
                                true,
                                true,
                                null,
                                null,
                                null,
                                Boolean.TRUE
                        )
                ))
                .build();

        Onboarding2OnboardingDroolsMapper mapper =
                new Onboarding2OnboardingDroolsMapper();

        // WHEN
        OnboardingDroolsDTO result = mapper.apply(onboarding);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertEquals("1", result.getInitiativeId());
        Assertions.assertTrue(result.isTc());
        Assertions.assertEquals("OK", result.getStatus());
        Assertions.assertTrue(result.getPdndAccept());
        Assertions.assertEquals(now, result.getTcAcceptTimestamp());
        Assertions.assertEquals(now, result.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(new BigDecimal(100), result.getIsee());

        Assertions.assertSame(onboarding.getResidence(), result.getResidence());
        Assertions.assertSame(onboarding.getBirthDate(), result.getBirthDate());
        Assertions.assertSame(onboarding.getFamily(), result.getFamily());
        Assertions.assertSame(onboarding.getChannel(), result.getChannel());
        Assertions.assertSame(onboarding.getVerifies(), result.getVerifies());

        Assertions.assertNotNull(result.getOnboardingRejectionReasons());

        TestUtils.checkNotNullFields(
                result,
                "serviceId",
                "userMail",
                "verifies"
        );
    }
}