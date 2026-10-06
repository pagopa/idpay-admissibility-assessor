package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InitInitiativeCounterServiceImplTest {

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    @InjectMocks
    private InitInitiativeCounterServiceImpl initInitiativeCounterService;

    private final InitiativeConfig initiative;

    public InitInitiativeCounterServiceImplTest() {
        this.initiative = InitiativeConfig.builder()
                .initiativeId("ID")
                .initiativeBudgetCents(1_00L)   // unico budget rimasto
                .build();
    }

    @BeforeEach
    void initMocks() {
        Mockito.when(initiativeCountersRepositoryMock.findById("ID"))
                .thenReturn(Mono.empty());

        Mockito.when(initiativeCountersRepositoryMock.save(Mockito.any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));
    }

    @Test
    void testNotExistent() {
        test(0L, 0L);
    }

    @Test
    void testAlreadyExistentCounter() {

        InitiativeCounters preSaved = InitiativeCounters.builder()
                .id(initiative.getInitiativeId())
                .onboarded(10L)
                .initiativeBudgetCents(1_00L)
                .reservedInitiativeBudgetCents(50L)
                .residualInitiativeBudgetCents(50L)
                .build();

        Mockito.when(initiativeCountersRepositoryMock.findById("ID"))
                .thenReturn(Mono.just(preSaved));

        test(preSaved.getOnboarded(), preSaved.getReservedInitiativeBudgetCents());
    }

    private void test(Long expectedOnboarded, Long expectedReservedCents) {

        InitiativeCounters result =
                initInitiativeCounterService.initCounters(initiative).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiative.getInitiativeId(), result.getId());
        Assertions.assertEquals(
                initiative.getInitiativeBudgetCents(),
                result.getInitiativeBudgetCents()
        );

        checkCounters(result, expectedOnboarded, expectedReservedCents);

        Mockito.verify(initiativeCountersRepositoryMock)
                .save(Mockito.same(result));
    }

    private void checkCounters(
            InitiativeCounters result,
            Long expectedOnboarded,
            Long expectedReservedCents) {

        Assertions.assertEquals(expectedOnboarded, result.getOnboarded());
        Assertions.assertEquals(expectedReservedCents, result.getReservedInitiativeBudgetCents());

        // residual = initiative budget - reserved
        Assertions.assertEquals(
                initiative.getInitiativeBudgetCents() - expectedReservedCents,
                result.getResidualInitiativeBudgetCents()
        );
    }
}
