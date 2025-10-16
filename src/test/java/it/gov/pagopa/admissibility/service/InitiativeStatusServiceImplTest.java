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
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .residualInitiativeBudgetCents(910_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isBudgetAvailable());
    }

    @Test
    void testBudgetExhausted() {
        // Given
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .residualInitiativeBudgetCents(0L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
    }

    @Test
    void testResidualBudgetIsNull() {
        // Given
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .residualInitiativeBudgetCents(null)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
    }

    @Test
    void testInitiativeCountersMissing() {
        // Given
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.empty());

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1")
                .blockOptional()
                .orElse(new InitiativeStatusDTO("STATUS1", false));

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
    }

    private static InitiativeConfig getInitiativeConfigForContextMock() {
        return InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudgetCents(1_000_000L)
                .status("STATUS1")
                .build();
    }
}
