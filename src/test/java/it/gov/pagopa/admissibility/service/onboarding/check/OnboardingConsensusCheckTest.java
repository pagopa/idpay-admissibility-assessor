package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class OnboardingConsensusCheckTest {

    @Test
    void testTcConsensusFalse() {
        // Given
        Instant timeMock = Instant.now();

        OnboardingDTO onboardingTcFalseMock = buildOnboardingRequest(false, true, timeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultTcFalse = onboardingConsensusCheck.apply(onboardingTcFalseMock, null, null);

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
        Instant timeMock = Instant.now();

        OnboardingDTO onboardingPdndFalseMock = buildOnboardingRequest(true, false, timeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultPdndFalse = onboardingConsensusCheck.apply(onboardingPdndFalseMock, null, null);

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

        Instant timeMock = Instant.now();

        OnboardingDTO onboardingSelfDeclarationFalseMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListFalseMock,
                timeMock,
                timeMock,
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
        Instant timeMock = Instant.now();

        OnboardingDTO onboardingSelfDeclarationFalseMock = buildOnboardingRequest(true, true, timeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultSelfDeclarationNull = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null, null);

        // Then
        assertNull(resultSelfDeclarationNull);

    }

    @Test
    void testConsensusTrue() {
        // Given
        Instant timeMock = Instant.now();

        OnboardingDTO onboardingMock = buildOnboardingRequest(true, true, timeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result = onboardingConsensusCheck.apply(onboardingMock, null, null);

        // Then
        assertNull(result);
    }

    private OnboardingDTO buildOnboardingRequest(boolean tc, boolean pdndCheck, Instant timeMock) {
        return new OnboardingDTO(
                "1",
                "1",
                tc,
                "OK",
                pdndCheck,
                timeMock,
                timeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null,
                false,
                "SERVICE",
                Boolean.TRUE,
                "USERMAIL",
                "CHANNEL",
                "NAME",
                "SURNAME",
                Boolean.TRUE
        );
    }

}