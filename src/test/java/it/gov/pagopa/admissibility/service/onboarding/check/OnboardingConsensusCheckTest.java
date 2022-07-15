package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                null,
                LocalDate.of(2000,1,1)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String resultTcFalse = onboardingConsensusCheck.apply(onboardingTcFalseMock, null);

        // Then
        assertEquals("CONSENSUS_CHECK_TC_FAIL", resultTcFalse);

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
                null,
                LocalDate.of(2000,1,1)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String resultPdndFalse = onboardingConsensusCheck.apply(onboardingPdndFalseMock, null);

        // Then
        assertEquals("CONSENSUS_CHECK_PDND_FAIL", resultPdndFalse);

    }

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
                null,
                LocalDate.of(2000,1,1)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String resultSelfDeclarationFalse = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null);

        // Then
        assertEquals("CONSENSUS_CHECK_SELF_DECLARATION_MAP_FAIL", resultSelfDeclarationFalse);

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
                null,
                LocalDate.of(2000,1,1)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String result = onboardingConsensusCheck.apply(onboardingMock, null);

        // Then
        assertNull(result);
    }

}