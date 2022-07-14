package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class OnboardingConsensusCheckTest {

    @Test
    void testConsensusFalse() {
        // Given
        Map<String, Boolean> selfDeclarationListTrueMock = new HashMap<>();
        selfDeclarationListTrueMock.put("MAP", true);

        Map<String, Boolean> selfDeclarationListFalseMock = new HashMap<>();
        selfDeclarationListFalseMock.put("MAP", false);

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
                new BigDecimal(100)
        );
        OnboardingDTO onboardingPdndFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                false,
                selfDeclarationListTrueMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100)
        );
        OnboardingDTO onboardingSelfDeclarationFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListFalseMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String resultTcFalse = onboardingConsensusCheck.apply(onboardingTcFalseMock, null);
        String resultPdndFalse = onboardingConsensusCheck.apply(onboardingPdndFalseMock, null);
        String resultSelfDeclarationFalse = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null);

        // Then
        assertEquals("CONSENSUS_CHECK_TC_FAIL", resultTcFalse);
        assertEquals("CONSENSUS_CHECK_PDND_FAIL", resultPdndFalse);
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
                new BigDecimal(100)
        );

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        String result = onboardingConsensusCheck.apply(onboardingMock, null);

        // Then
        assertNull(result);
    }

}