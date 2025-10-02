package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingInitiativeEndDateCheckTest {

    private OnboardingInitiativeEndDateCheck check;

    @BeforeEach
    void setUp() {
        check = new OnboardingInitiativeEndDateCheck();
    }

    @Test
    void testNullInitiativeConfig() {
        // given
        OnboardingDTO onboardingDTO = Mockito.mock(OnboardingDTO.class);
        Mockito.when(onboardingDTO.getUserId()).thenReturn("user1");
        Mockito.when(onboardingDTO.getInitiativeId()).thenReturn("initiative1");

        // when
        OnboardingRejectionReason result = check.apply(onboardingDTO, null, new HashMap<>());

        // then
        assertNotNull(result);
        assertEquals(OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL, result.getCode());
        assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST, result.getType());
    }

    @Test
    void testInitiativeNotEndedYet() {
        // given
        OnboardingDTO onboardingDTO = Mockito.mock(OnboardingDTO.class);
        Mockito.when(onboardingDTO.getUserId()).thenReturn("user2");
        Mockito.when(onboardingDTO.getInitiativeId()).thenReturn("initiative2");

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setEndDate(LocalDate.now().plusDays(5)); // futura → non ancora terminata

        // when
        OnboardingRejectionReason result = check.apply(onboardingDTO, initiativeConfig, Map.of());

        // then
        assertNotNull(result);
        assertEquals(OnboardingConstants.REJECTION_REASON_INITIATIVE_ENDED, result.getCode());
        assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST, result.getType());
    }

    @Test
    void testInitiativeEnded() {
        // given
        OnboardingDTO onboardingDTO = Mockito.mock(OnboardingDTO.class);
        Mockito.when(onboardingDTO.getUserId()).thenReturn("user3");
        Mockito.when(onboardingDTO.getInitiativeId()).thenReturn("initiative3");

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setEndDate(LocalDate.now().minusDays(1)); // già terminata

        // when
        OnboardingRejectionReason result = check.apply(onboardingDTO, initiativeConfig, Map.of());

        // then
        assertNull(result, "Se l’iniziativa è già terminata, non deve esserci motivo di rifiuto");
    }
}

