package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class InitiativeStatusServiceImplTest {

    @Test
    void test() {
        // Given
        InitiativeCountersRepository initiativeCountersRepositoryMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudget(BigDecimal.ONE)
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .status("STATUS1")
                .build();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(Mockito.anyString())).thenReturn(Mono.just(initiativeConfigMock));

        InitiativeCounters initiativeCountersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .initiativeBudgetCents(100L)
                .onboarded(1L)
                .residualInitiativeBudgetCents(100000L)
                .reservedInitiativeBudgetCents(1000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.just(initiativeCountersMock));

        InitiativeStatusService initiativeStatusService = new InitiativeStatusServiceImpl(onboardingContextHolderServiceMock, initiativeCountersRepositoryMock);

        InitiativeStatusDTO expected = new InitiativeStatusDTO("STATUS1", true);

        // When
        InitiativeStatusDTO result = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testNoInitiative() {
        // Given
        InitiativeCountersRepository initiativeCountersRepositoryMock = Mockito.mock(InitiativeCountersRepository.class);
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudget(BigDecimal.ONE)
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .status("STATUS1")
                .build();
        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(Mockito.anyString())).thenReturn(Mono.just(initiativeConfigMock));

        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.empty());

        InitiativeStatusService initiativeStatusService = new InitiativeStatusServiceImpl(onboardingContextHolderServiceMock, initiativeCountersRepositoryMock);

        // When
        InitiativeStatusDTO result = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNull(result);
    }
}