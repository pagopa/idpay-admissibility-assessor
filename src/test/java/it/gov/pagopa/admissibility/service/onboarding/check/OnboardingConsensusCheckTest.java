package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class OnboardingConsensusCheckTest {

    @Test
    void testTcConsensusFalse() {
        // Given
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingTcFalseMock = buildOnboardingRequest(false, true, localDateTimeMock);

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
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingPdndFalseMock = buildOnboardingRequest(true, false, localDateTimeMock);

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

        OnboardingDTO onboardingSelfDeclarationFalseMock = buildOnboardingRequest(true, true, localDateTimeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason resultSelfDeclarationNull = onboardingConsensusCheck.apply(onboardingSelfDeclarationFalseMock, null, null);

        // Then
        assertNull(resultSelfDeclarationNull);

    }

    @Test
    void testConsensusTrue() {
        // Given
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingMock = buildOnboardingRequest(true, true, localDateTimeMock);

        OnboardingConsensusCheck onboardingConsensusCheck = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result = onboardingConsensusCheck.apply(onboardingMock, null, null);

        // Then
        assertNull(result);
    }

    private OnboardingDTO buildOnboardingRequest(boolean tc, boolean pdndCheck, LocalDateTime localDateTimeMock) {
        return new OnboardingDTO(
                "1",
                "1",
                tc,
                "OK",
                pdndCheck,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null,
                false,
                "SERVICE",
                Boolean.TRUE
        );
    }

}