package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.service.onboarding.check.OnboardingCheck;
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
        OnboardingCheck checkMock1 = configureOnboardingCheckMock(null);
        OnboardingCheck checkMock2 = configureOnboardingCheckMock(null);
        OnboardingCheck checkMock3 = configureOnboardingCheckMock("Failing mock 3");
        OnboardingCheck checkMock4 = Mockito.mock(OnboardingCheck.class);
        List<OnboardingCheck> checkMocks = Arrays.asList(checkMock1, checkMock2, checkMock3, checkMock4);
        OnboardingCheckService OnboardingCheckService = new OnboardingCheckServiceImpl(checkMocks);

        OnboardingDTO trx = Mockito.mock(OnboardingDTO.class);

        // When
        String result = OnboardingCheckService.check(trx);

        // Then
        Assertions.assertEquals("Failing mock 3", result);

        Mockito.verify(checkMock1).apply(Mockito.same(trx));
        Mockito.verify(checkMock2).apply(Mockito.same(trx));
        Mockito.verify(checkMock3).apply(Mockito.same(trx));

        Mockito.verify(checkMock4,Mockito.never()).apply(Mockito.any());

        Mockito.verifyNoMoreInteractions(checkMock1, checkMock2, checkMock3, checkMock4);

    }

    @Test
    void testFilterTrue() {
        // Given
        OnboardingCheck checkMock1 = configureOnboardingCheckMock(null);
        OnboardingCheck checkMock2 = configureOnboardingCheckMock(null);
        OnboardingCheck checkMock3 = configureOnboardingCheckMock(null);
        OnboardingCheck checkMock4 = configureOnboardingCheckMock(null);
        List<OnboardingCheck> checkMocks = Arrays.asList(checkMock1,checkMock2, checkMock3, checkMock4);

        OnboardingCheckService OnboardingCheckService = new OnboardingCheckServiceImpl(checkMocks);

        OnboardingDTO trx = Mockito.mock(OnboardingDTO.class);

        // When
        String result = OnboardingCheckService.check(trx);

        // Then
        Assertions.assertNull(result);

        Mockito.verify(checkMock1).apply(Mockito.same(trx));
        Mockito.verify(checkMock2).apply(Mockito.same(trx));
        Mockito.verify(checkMock3).apply(Mockito.same(trx));
        Mockito.verify(checkMock4).apply(Mockito.same(trx));

        Mockito.verifyNoMoreInteractions(checkMock1, checkMock2, checkMock3, checkMock4);

    }

    private OnboardingCheck configureOnboardingCheckMock(String expectedRejection) {
        OnboardingCheck checkMock = Mockito.mock(OnboardingCheck.class);
        Mockito.when(checkMock.apply(Mockito.any())).thenReturn(expectedRejection);
        return checkMock;
    }
}