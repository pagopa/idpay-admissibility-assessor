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

    public InitInitiativeCounterServiceImplTest(){
        this.initiative = InitiativeConfig.builder()
                .initiativeId("ID")
                .initiativeBudgetCents(1_00L)
                .beneficiaryInitiativeBudgetCents(10_00L)
                .build();
    }

    @BeforeEach
    void initMocks(){
        Mockito.when(initiativeCountersRepositoryMock.findById("ID")).thenReturn(Mono.empty());
        Mockito.when(initiativeCountersRepositoryMock.save(Mockito.any())).thenAnswer(i->Mono.just(i.getArgument(0)));
    }

    @Test
    void testNotExistent(){
        test(0L, 0L);
    }

    @Test
    void testAlreadyExistentCounter(){
        InitiativeCounters preSaved = InitiativeCounters.builder()
                .id(initiative.getInitiativeId())
                .onboarded(10L)
                .initiativeBudgetCents(10000L)
                .residualInitiativeBudgetCents(10000L)
                .build();

        Mockito.when(initiativeCountersRepositoryMock.findById("ID")).thenReturn(Mono.just(preSaved));

        test(preSaved.getOnboarded(), preSaved.getReservedInitiativeBudgetCents());
    }

    private void test(Long expectedOnboarded, Long expectedReservationCents) {
        final InitiativeCounters result = initInitiativeCounterService.initCounters(initiative).block();

        Assertions.assertNotNull(result);
        Assertions.assertSame(initiative.getInitiativeId(), result.getId());
        Assertions.assertEquals(initiative.getInitiativeBudgetCents(), result.getInitiativeBudgetCents());

        checkCounters(result, expectedOnboarded, expectedReservationCents);

        Mockito.verify(initiativeCountersRepositoryMock).save(Mockito.same(result));
    }

    private void checkCounters(InitiativeCounters result, Long expectedOnboarded, Long expectedReservationCents) {
        Assertions.assertEquals(expectedOnboarded, result.getOnboarded());
        Assertions.assertEquals(expectedReservationCents, result.getReservedInitiativeBudgetCents());
        Assertions.assertEquals(initiative.getInitiativeBudgetCents(), result.getResidualInitiativeBudgetCents());
    }

}
