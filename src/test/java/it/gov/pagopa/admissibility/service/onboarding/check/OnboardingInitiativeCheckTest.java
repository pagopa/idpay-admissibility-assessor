package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OnboardingInitiativeCheckTest {

    @Test
    void testInitiativeNotFound() {

        // Given
        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId(null)
                .tcAcceptTimestamp(LocalDateTime.now())
                .criteriaConsensusTimestamp(LocalDateTime.now())
                .build();

        Map<String, Object> context = new HashMap<>();

        OnboardingInitiativeCheck check = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, null, context);

        // Then
        OnboardingRejectionReason expected =
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code("INVALID_INITIATIVE_ID")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void testInitiativeTcDateFail() {

        // Given
        LocalDateTime tcAccept = LocalDateTime.of(2022, 1, 1, 0, 0);

        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId("INITIATIVE")
                .tcAcceptTimestamp(tcAccept)
                .criteriaConsensusTimestamp(tcAccept)
                .build();

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        Mockito.when(initiativeConfig.getStartDate())
                .thenReturn(LocalDate.of(2021, 1, 1));
        Mockito.when(initiativeConfig.getEndDate())
                .thenReturn(LocalDate.of(2021, 12, 31));

        OnboardingInitiativeCheck check = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, initiativeConfig, new HashMap<>());

        // Then
        OnboardingRejectionReason expected =
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code("CONSENSUS_CHECK_TC_ACCEPT_FAIL")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void testInitiativeCriteriaConsensusDateFail() {

        // Given
        LocalDateTime tcAccept = LocalDateTime.of(2021, 7, 14, 0, 0);
        LocalDateTime criteriaConsensus = LocalDateTime.of(2022, 1, 1, 0, 0);

        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId("INITIATIVE")
                .tcAcceptTimestamp(tcAccept)
                .criteriaConsensusTimestamp(criteriaConsensus)
                .build();

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        Mockito.when(initiativeConfig.getStartDate())
                .thenReturn(LocalDate.of(2021, 1, 1));
        Mockito.when(initiativeConfig.getEndDate())
                .thenReturn(LocalDate.of(2021, 12, 31));

        OnboardingInitiativeCheck check = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, initiativeConfig, new HashMap<>());

        // Then
        OnboardingRejectionReason expected =
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code("CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void testInitiativeCheckOk() {

        // Given
        LocalDateTime now = LocalDateTime.now();

        OnboardingDTO onboarding = OnboardingDTO.builder()
                .userId("1")
                .initiativeId("INITIATIVE")
                .tcAcceptTimestamp(now)
                .criteriaConsensusTimestamp(now)
                .build();

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setStartDate(LocalDate.now().minusDays(1));
        initiativeConfig.setEndDate(LocalDate.now().plusDays(1));

        OnboardingInitiativeCheck check = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result =
                check.apply(onboarding, initiativeConfig, new HashMap<>());

        // Then
        assertNull(result);
    }
}
