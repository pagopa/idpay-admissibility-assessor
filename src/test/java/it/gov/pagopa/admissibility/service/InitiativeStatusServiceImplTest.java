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
                .spentInitiativeBudgetCents(90_000L)
                .residualInitiativeBudgetCents(200_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isBudgetAvailable());
        Assertions.assertTrue(result.isResidualBudgetAvailable());
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
                .spentInitiativeBudgetCents(995_000L)
                .residualInitiativeBudgetCents(5_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
        Assertions.assertFalse(result.isResidualBudgetAvailable());
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
                .spentInitiativeBudgetCents(null)
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
        Assertions.assertFalse(result.isResidualBudgetAvailable());
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
                .orElse(new InitiativeStatusDTO("STATUS1", false, false));

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
        Assertions.assertFalse(result.isResidualBudgetAvailable());
    }

    @Test
    void testInitiativeBudgetCentsIsNull() {
        // Given
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfig = getInitiativeConfigForContextMock();
        initiativeConfig.setInitiativeBudgetCents(null);
        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfig));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(100_000L)
                .residualInitiativeBudgetCents(200_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBudgetAvailable());
        Assertions.assertTrue(result.isResidualBudgetAvailable());
    }

    @Test
    void testResidualBudgetNull() {
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
        Assertions.assertFalse(result.isResidualBudgetAvailable());
    }

    @Test
    void testBeneficiaryBudgetNull() {
        // Given
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfig = getInitiativeConfigForContextMock();
        initiativeConfig.setBeneficiaryBudgetFixedCents(null);
        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfig));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .residualInitiativeBudgetCents(50_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        // When
        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isResidualBudgetAvailable());
    }

    @Test
    void testSanitizeForLog_NullInput() throws Exception {
        var method = InitiativeStatusServiceImpl.class.getDeclaredMethod("sanitizeForLog", String.class);
        method.setAccessible(true);

        Object result = method.invoke(null, (Object) null);

        Assertions.assertNull(result);
    }

    @Test
    void testBudgetAvailable_WhenResidualIsExactlyThreshold() {
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(990_000L)
                .residualInitiativeBudgetCents(100_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isBudgetAvailable());
        Assertions.assertEquals("STATUS1", result.getStatus());
    }

    @Test
    void testResidualBudgetAvailable_UsesBeneficiaryBudgetMaxWhenPresent() {
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfig = getInitiativeConfigForContextMock();
        initiativeConfig.setBeneficiaryBudgetFixedCents(100_000L);
        initiativeConfig.setBeneficiaryBudgetMaxCents(50_000L);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(initiativeConfig));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(100_000L)
                .residualInitiativeBudgetCents(60_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isResidualBudgetAvailable());
    }

    @Test
    void testResidualBudgetAvailable_WhenResidualEqualsBeneficiaryBudget_ReturnsTrue() {
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        InitiativeCounters countersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .spentInitiativeBudgetCents(200_000L)
                .residualInitiativeBudgetCents(100_000L)
                .build();
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.just(countersMock));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        InitiativeStatusDTO result = service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isResidualBudgetAvailable());
    }

    @Test
    void testGetInitiativeStatusAndBudgetAvailable_WhenContextFails_PropagatesError() {
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        RuntimeException expected = new RuntimeException("context error");
        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.error(expected));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class,
                () -> service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block());

        Assertions.assertSame(expected, thrown);
        Mockito.verifyNoInteractions(repoMock);
    }

    @Test
    void testGetInitiativeStatusAndBudgetAvailable_WhenRepositoryFails_PropagatesError() {
        InitiativeCountersRepository repoMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService contextMock = Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextMock.getInitiativeConfig(Mockito.anyString()))
                .thenReturn(Mono.just(getInitiativeConfigForContextMock()));

        RuntimeException expected = new RuntimeException("repository error");
        Mockito.when(repoMock.findById(Mockito.anyString()))
                .thenReturn(Mono.error(expected));

        InitiativeStatusService service = new InitiativeStatusServiceImpl(contextMock, repoMock);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class,
                () -> service.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block());

        Assertions.assertSame(expected, thrown);
    }

    @Test
    void testSanitizeForLog_SpecialCharacters() throws Exception {
        var method = InitiativeStatusServiceImpl.class.getDeclaredMethod("sanitizeForLog", String.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "INITIATIVE:1 / test@status");

        Assertions.assertEquals("INITIATIVE_1___test_status", result);
    }

    private static InitiativeConfig getInitiativeConfigForContextMock() {
        return InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudgetCents(1_000_000L)
                .beneficiaryBudgetFixedCents(100_000L)
                .status("STATUS1")
                .build();
    }
}
