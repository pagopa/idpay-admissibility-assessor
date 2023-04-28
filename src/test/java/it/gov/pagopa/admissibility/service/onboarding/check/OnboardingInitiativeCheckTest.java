package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
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
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                null,
                true,
                "OK",
                true,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result = onboardingInitiativeCheck.apply(onboardingMock, null, onboardingContext);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("INVALID_INITIATIVE_ID")
                .build();
        assertEquals(expected, result);
    }

    @Test
    void testInitiativeTcDateFail() {

        // Given
        LocalDateTime localDateTimeMock = LocalDateTime.of(2022,1,1,0,0);

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021,1,1));
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021,12,31));

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result = onboardingInitiativeCheck.apply(onboardingMock, initiativeConfig, onboardingContext);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("CONSENSUS_CHECK_TC_ACCEPT_FAIL")
                .build();
        assertEquals(expected, result);
    }

    @Test
    void testInitiativeCriteriaConsensusDateFail() {

        // Given
        LocalDateTime localDateTimeMock1 = LocalDateTime.of(2021,7,14,0,0);
        LocalDateTime localDateTimeMock2 = LocalDateTime.of(2022,1,1,0,0);

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                localDateTimeMock1,
                localDateTimeMock2,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021,1,1));
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021,12,31));

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result = onboardingInitiativeCheck.apply(onboardingMock, initiativeConfig, onboardingContext);

        // Then
        OnboardingRejectionReason expected = OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL")
                .build();
        assertEquals(expected, result);
    }

    @Test
    void testInitiativeCheckOk() {

        // Given
        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboarding = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                null
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        final InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setStartDate(LocalDate.now());

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result = onboardingInitiativeCheck.apply(onboarding, initiativeConfig, onboardingContext);

        // Then
        assertNull(result);
    }
}