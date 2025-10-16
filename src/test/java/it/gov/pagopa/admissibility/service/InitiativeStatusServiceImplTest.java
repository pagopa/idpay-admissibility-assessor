package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InitiativeStatusServiceImplTest {

    @Test
    void testBudgetAvailable() {
        // Given
        InitiativeCountersRepository initiativeCountersRepositoryMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfigMock = getInitiativeConfigForContextMock();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfigMock));

        InitiativeCounters initiativeCountersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(90_000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeCountersMock));

        InitiativeStatusService initiativeStatusService =
                new InitiativeStatusServiceImpl(onboardingContextHolderServiceMock, initiativeCountersRepositoryMock);

        InitiativeStatusDTO expected = new InitiativeStatusDTO("STATUS1", true);

        // When
        InitiativeStatusDTO result = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testBudgetExhausted() {
        // Given
        InitiativeCountersRepository initiativeCountersRepositoryMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfigMock = getInitiativeConfigForContextMock();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfigMock));

        InitiativeCounters initiativeCountersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(995_000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeCountersMock));

        InitiativeStatusService initiativeStatusService =
                new InitiativeStatusServiceImpl(onboardingContextHolderServiceMock, initiativeCountersRepositoryMock);

        InitiativeStatusDTO expected = new InitiativeStatusDTO("STATUS1", false);

        // When
        InitiativeStatusDTO result = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testBudgetInfoMissing() {
        // Given
        InitiativeCountersRepository initiativeCountersRepositoryMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfigMock1 = InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudgetCents(null)
                .status("STATUS1")
                .build();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfigMock1));

        InitiativeCounters initiativeCountersMock1 = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(100_000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeCountersMock1));

        InitiativeStatusService initiativeStatusService =
                new InitiativeStatusServiceImpl(onboardingContextHolderServiceMock, initiativeCountersRepositoryMock);

        InitiativeStatusDTO result1 = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        Assertions.assertFalse(result1.isBudgetAvailable());

        InitiativeConfig initiativeConfigMock2 = InitiativeConfig.builder()
                .initiativeId("INITIATIVE2")
                .initiativeBudgetCents(1_000_000L)
                .status("STATUS2")
                .build();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig("INITIATIVE2"))
                .thenReturn(Mono.just(initiativeConfigMock2));

        InitiativeCounters initiativeCountersMock2 = InitiativeCounters.builder()
                .id("INITIATIVE2")
                .spentInitiativeBudgetCents(null)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById("INITIATIVE2"))
                .thenReturn(Mono.just(initiativeCountersMock2));

        InitiativeStatusDTO result2 = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE2").block();

        Assertions.assertFalse(result2.isBudgetAvailable());
    }

    private static InitiativeConfig getInitiativeConfigForContextMock() {
        return InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudgetCents(1_000_000L)
                .status("STATUS1")
                .build();
    }
}
