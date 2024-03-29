package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.check.OnboardingCheck;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;


@ExtendWith(MockitoExtension.class)
@Slf4j
class OnboardingCheckServiceImplTest {

    @Test
    void testFilterFalse() {
        // Given
        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId("INITIATIVEID").build();
        OnboardingRejectionReason expectedRejectionReason = OnboardingRejectionReason.builder().type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR).code("Failing mock 3").build();

        OnboardingCheck checkMock1 = configureOnboardingCheckMock(null, initiativeConfig);
        OnboardingCheck checkMock2 = configureOnboardingCheckMock(null, initiativeConfig);
        OnboardingCheck checkMock3 = configureOnboardingCheckMock(expectedRejectionReason, initiativeConfig);
        OnboardingCheck checkMock4 = Mockito.mock(OnboardingCheck.class);
        List<OnboardingCheck> checkMocks = Arrays.asList(checkMock1, checkMock2, checkMock3, checkMock4);
        OnboardingCheckService OnboardingCheckService = new OnboardingCheckServiceImpl(checkMocks);

        OnboardingDTO trx = Mockito.mock(OnboardingDTO.class);

        // When
        OnboardingRejectionReason result = OnboardingCheckService.check(trx, initiativeConfig, null);

        // Then
        Assertions.assertEquals(expectedRejectionReason, result);

        Mockito.verify(checkMock1).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(checkMock2).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(checkMock3).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());

        Mockito.verify(checkMock4,Mockito.never()).apply(Mockito.any(), Mockito.same(initiativeConfig), Mockito.any());

        Mockito.verifyNoMoreInteractions(checkMock1, checkMock2, checkMock3, checkMock4);

    }

    @Test
    void testFilterTrue() {
        // Given
        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId("INITIATIVEID").build();

        OnboardingCheck checkMock1 = configureOnboardingCheckMock(null, initiativeConfig);
        OnboardingCheck checkMock2 = configureOnboardingCheckMock(null, initiativeConfig);
        OnboardingCheck checkMock3 = configureOnboardingCheckMock(null, initiativeConfig);
        OnboardingCheck checkMock4 = configureOnboardingCheckMock(null, initiativeConfig);
        List<OnboardingCheck> checkMocks = Arrays.asList(checkMock1,checkMock2, checkMock3, checkMock4);

        OnboardingCheckService OnboardingCheckService = new OnboardingCheckServiceImpl(checkMocks);

        OnboardingDTO trx = Mockito.mock(OnboardingDTO.class);

        // When
        OnboardingRejectionReason result = OnboardingCheckService.check(trx, initiativeConfig, null);

        // Then
        Assertions.assertNull(result);

        Mockito.verify(checkMock1).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(checkMock2).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(checkMock3).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(checkMock4).apply(Mockito.same(trx), Mockito.same(initiativeConfig), Mockito.any());

        Mockito.verifyNoMoreInteractions(checkMock1, checkMock2, checkMock3, checkMock4);

    }

    private OnboardingCheck configureOnboardingCheckMock(OnboardingRejectionReason expectedRejection, InitiativeConfig initiativeConfig) {
        OnboardingCheck checkMock = Mockito.mock(OnboardingCheck.class);
        Mockito.when(checkMock.apply(Mockito.any(), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(expectedRejection);
        return checkMock;
    }
}