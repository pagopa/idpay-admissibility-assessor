package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingInitiativeCheckTest {

    @Test
    void testInitiativeNotFound() {

        // Given
        Map<String, Boolean> selfDeclarationListMock = new HashMap<>();
        selfDeclarationListMock.put("MAP", true);

        LocalDateTime localDateTimeMock = LocalDateTime.now();

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                null,
                true,
                "OK",
                true,
                selfDeclarationListMock,
                localDateTimeMock,
                localDateTimeMock,
                new BigDecimal(100)
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        OnboardingContextHolderService onboardingContextHolder = Mockito.mock(OnboardingContextHolderServiceImpl.class);

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck(onboardingContextHolder);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertEquals("INVALID_INITIATIVE_ID", result);
    }

    @Test
    void testInitiativeTcDateFail() {

        // Given
        Map<String, Boolean> selfDeclarationListMock = new HashMap<>();
        selfDeclarationListMock.put("MAP", true);

        LocalDateTime localDateTimeMock = LocalDateTime.of(2022,1,1,0,0);

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

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        OnboardingContextHolderService onboardingContextHolder = Mockito.mock(OnboardingContextHolderServiceImpl.class);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(onboardingContextHolder.getInitiativeConfig(onboardingMock.getInitiativeId())).thenReturn(initiativeConfig);
        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021,1,1));
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021,12,31));

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck(onboardingContextHolder);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertEquals("CONSENSUS_CHECK_TC_ACCEPT_FAIL", result);
    }

    @Test
    void testInitiativeCriteriaConsensusDateFail() {

        // Given
        Map<String, Boolean> selfDeclarationListMock = new HashMap<>();
        selfDeclarationListMock.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.of(2021,7,14,0,0);
        LocalDateTime localDateTimeMock2 = LocalDateTime.of(2022,1,1,0,0);

        OnboardingDTO onboardingMock = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListMock,
                localDateTimeMock1,
                localDateTimeMock2,
                new BigDecimal(100)
        );

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        OnboardingContextHolderService onboardingContextHolder = Mockito.mock(OnboardingContextHolderServiceImpl.class);

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);

        Mockito.when(onboardingContextHolder.getInitiativeConfig(onboardingMock.getInitiativeId())).thenReturn(initiativeConfig);
        Mockito.when(initiativeConfig.getStartDate()).thenReturn(LocalDate.of(2021,1,1));
        Mockito.when(initiativeConfig.getEndDate()).thenReturn(LocalDate.of(2021,12,31));

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck(onboardingContextHolder);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertEquals("CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL", result);
    }

    @Test
    void testInitiativeCheckOk() {

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

        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(null, null);

        OnboardingInitiativeCheck onboardingInitiativeCheck = Mockito.mock(OnboardingInitiativeCheck.class);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertNull(result);
    }
}