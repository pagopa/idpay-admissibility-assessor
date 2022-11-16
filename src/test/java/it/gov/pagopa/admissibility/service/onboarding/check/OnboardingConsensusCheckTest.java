package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class OnboardingConsensusCheckTest {

    @Test
    void testTcConsensusFalse() {

        // Given
        Map<String, Boolean> selfDeclarationListTrueMock = new HashMap<>();
        selfDeclarationListTrueMock.put("MAP", true);

        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingTcFalseMock = new OnboardingDTO(
                "1",
                "1",
                false,
                "OK",
                true,
                selfDeclarationListTrueMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultTcFalse = onboardingConsensusCheck.apply(onboardingTcFalseMock, null);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                .code("CONSENSUS_CHECK_TC_FAIL")
                .build();
        assertEquals(expected, resultTcFalse);

    }

    @Test
    void testPdndConsensusFalse() {

        // Given
        Map<String, Boolean> selfDeclarationListTrueMock = new HashMap<>();
        selfDeclarationListTrueMock.put("MAP", true);

        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingPdndFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                false,
                selfDeclarationListTrueMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultPdndFalse = onboardingConsensusCheck.apply(onboardingPdndFalseMock, null);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                .code("CONSENSUS_CHECK_PDND_FAIL")
                .build();
        assertEquals(expected, resultPdndFalse);

    }

    //Handle multi and boolean criteria
    /*
    @Test
    void testSelfDeclarationConsensusFalse() {

        // Given
        Map<String, Boolean> selfDeclarationListFalseMock = new HashMap<>();
        selfDeclarationListFalseMock.put("MAP", false);

        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingSelfDeclarationFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListFalseMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultSelfDeclarationFalse = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                .code("CONSENSUS_CHECK_SELF_DECLARATION_MAP_FAIL")
                .build();
        assertEquals(expected, resultSelfDeclarationFalse);

    }
    */

    @Test
    void testSelfDeclarationConsensusNull() {

        // Given
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingSelfDeclarationFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                null,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultSelfDeclarationNull = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null);

        // Then
        assertNull(resultSelfDeclarationNull);

    }

    @Test
    void testConsensusTrue() {
        // Given
        Map<String, Boolean> selfDeclarationListMock = new HashMap<>();
        selfDeclarationListMock.put("MAP", true);

        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result = onboardingConsensusCheck.apply(onboardingMock, null);

        // Then
        assertNull(result);
    }

}