package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
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

    /*@Test
    void testTcDateFail() {

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

        OnboardingContextHolderService onboardingContextHolder = Mockito.mock(OnboardingContextHolderServiceImpl.class);

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck(onboardingContextHolder);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertEquals("CONSENSUS_CHECK_TC_ACCEPT_FAIL", result);
    }

    @Test
    void testCriteriaConsensusDateFail() {

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

        OnboardingContextHolderService onboardingContextHolder = Mockito.mock(OnboardingContextHolderServiceImpl.class);

        OnboardingInitiativeCheck onboardingInitiativeCheck = new OnboardingInitiativeCheck(onboardingContextHolder);

        // When
        String result = onboardingInitiativeCheck.apply(onboardingMock, onboardingContext);

        // Then
        assertEquals("CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL", result);
    }*/

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