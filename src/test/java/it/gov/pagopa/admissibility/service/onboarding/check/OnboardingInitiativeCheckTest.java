package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OnboardingInitiativeCheckTest {

    private ZoneId zone = ZoneId.of("Europe/Rome");

    @Test
    void testInitiativeNotFound() {

        // Given
        Instant timeMock = Instant.now();

        OnboardingDTO onboardingMock = buildOnboardingRequest(null, timeMock);

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
        Instant timeMock = LocalDateTime.of(2022,1,1,0,0).atZone(zone).toInstant();

        OnboardingDTO onboardingMock = buildOnboardingRequest("1", timeMock);

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021, 1, 1)
                .atStartOfDay(ZoneId.of("Europe/Rome"))
                .toInstant()
        );
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021, 12, 31)
                .plusDays(1).atStartOfDay(ZoneId.of("Europe/Rome"))
                .minusNanos(1)
                .toInstant());

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
        Instant timeMock1 = LocalDateTime.of(2021,7,14,0,0).atZone(zone).toInstant();
        Instant timeMock2 = LocalDateTime.of(2022,1,1,0,0).atZone(zone).toInstant();

        OnboardingDTO onboardingMock = buildOnboardingRequest("1", timeMock1);
        onboardingMock.setCriteriaConsensusTimestamp(timeMock2);

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021, 1, 1)
                .atStartOfDay(ZoneId.of("Europe/Rome"))
                .toInstant()
        );
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021, 12, 31)
                .plusDays(1).atStartOfDay(ZoneId.of("Europe/Rome"))
                .minusNanos(1)
                .toInstant());

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
        Instant timeMock = Instant.now();

        OnboardingDTO onboarding = buildOnboardingRequest("1", timeMock);

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        final InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setStartDate(Instant.now());

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck();

        // When
        OnboardingRejectionReason result = onboardingInitiativeCheck.apply(onboarding, initiativeConfig, onboardingContext);

        // Then
        assertNull(result);
    }

    private OnboardingDTO buildOnboardingRequest(String initiativeId, Instant timeMock) {
        return new OnboardingDTO(
                "1",
                initiativeId,
                true,
                "OK",
                true,
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