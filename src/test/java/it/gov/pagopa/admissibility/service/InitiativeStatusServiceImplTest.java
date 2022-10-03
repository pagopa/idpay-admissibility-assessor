package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
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
        DroolsRuleRepository droolsRuleRepositoryMock = Mockito.mock(DroolsRuleRepository.class);

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudget(BigDecimal.ONE)
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .status("STATUS1")
                .build();
        DroolsRule droolsRuleMock = DroolsRule.builder()
                .id("ID1")
                .name("TEST1")
                .rule("RULE1")
                .initiativeConfig(initiativeConfigMock)
                .build();
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.just(droolsRuleMock));

        InitiativeCounters initiativeCountersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .initiativeBudgetCents(100L)
                .onboarded(1L)
                .residualInitiativeBudgetCents(100000L)
                .reservedInitiativeBudgetCents(1000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.just(initiativeCountersMock));

        InitiativeStatusService initiativeStatusService = new InitiativeStatusServiceImpl(droolsRuleRepositoryMock, initiativeCountersRepositoryMock);

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
        DroolsRuleRepository droolsRuleRepositoryMock = Mockito.mock(DroolsRuleRepository.class);

        InitiativeConfig initiativeConfigMock = InitiativeConfig.builder()
                .initiativeId("INITIATIVE1")
                .initiativeBudget(BigDecimal.ONE)
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .status("STATUS1")
                .build();
        DroolsRule droolsRuleMock = DroolsRule.builder()
                .id("ID1")
                .name("TEST1")
                .rule("RULE1")
                .initiativeConfig(initiativeConfigMock)
                .build();
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.just(droolsRuleMock));

        InitiativeCounters initiativeCountersMock = InitiativeCounters.builder()
                .id("INITIATIVE1")
                .initiativeBudgetCents(100L)
                .onboarded(1L)
                .residualInitiativeBudgetCents(100000L)
                .reservedInitiativeBudgetCents(1000L)
                .build();
        Mockito.when(initiativeCountersRepositoryMock.findById(Mockito.anyString())).thenReturn(Mono.empty());

        InitiativeStatusService initiativeStatusService = new InitiativeStatusServiceImpl(droolsRuleRepositoryMock, initiativeCountersRepositoryMock);

        // When
        InitiativeStatusDTO result = initiativeStatusService.getInitiativeStatusAndBudgetAvailable("INITIATIVE1").block();

        // Then
        Assertions.assertNull(result);
    }
}