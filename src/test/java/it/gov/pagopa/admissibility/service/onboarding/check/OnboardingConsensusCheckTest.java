package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OnboardingConsensusCheckTest {

    @Test
    void testTcConsensusFalse() {

        // Given
        OnboardingDTO onboarding = buildOnboarding(false, true);

        OnboardingConsensusCheck check = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, null, null);

        // Then
        OnboardingRejectionReason expected =
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                        .code("CONSENSUS_CHECK_TC_FAIL")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void testPdndConsensusFalse() {

        // Given
        OnboardingDTO onboarding = buildOnboarding(true, false);

        OnboardingConsensusCheck check = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, null, null);

        // Then
        OnboardingRejectionReason expected =
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                        .code("CONSENSUS_CHECK_PDND_FAIL")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void testConsensusAllTrue() {

        // Given
        OnboardingDTO onboarding = buildOnboarding(true, true);

        OnboardingConsensusCheck check = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, null, null);

        // Then
        assertNull(result);
    }

    @Test
    void testConsensusTimestampNull() {

        // Given
        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId("1")
                .tc(true)
                .pdndAccept(true)
                .criteriaConsensusTimestamp(null)
                .build();

        OnboardingConsensusCheck check = new OnboardingConsensusCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, null, null);

        // Then
        assertNull(result);
    }



    private OnboardingDTO buildOnboarding(boolean tc, boolean pdndAccept) {
        return OnboardingDTO.builder()
                .userId("1")
                .initiativeId("1")
                .tc(tc)
                .pdndAccept(pdndAccept)
                .criteriaConsensusTimestamp(LocalDateTime.now())
                .build();
    }
}