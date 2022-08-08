package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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
                .initiativeBudget(BigDecimal.ONE)
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .build();
    }

    @BeforeEach
    private void initMocks(){
        Mockito.when(initiativeCountersRepositoryMock.findById("ID")).thenReturn(Mono.empty());
        Mockito.when(initiativeCountersRepositoryMock.save(Mockito.any())).thenAnswer(i->Mono.just(i.getArgument(0)));
    }

    @Test
    void testNotExistent(){
        test(0L, BigDecimal.ZERO);
    }

    @Test
    void testAlreadyExistentCounter(){
        InitiativeCounters preSaved = InitiativeCounters.builder()
                .id(initiative.getInitiativeId())
                .onboarded(10L)
                .initiativeBudget(BigDecimal.TEN)
                .build();

        Mockito.when(initiativeCountersRepositoryMock.findById("ID")).thenReturn(Mono.just(preSaved));

        test(preSaved.getOnboarded(), preSaved.getReservedInitiativeBudget());
    }

    private void test(Long expectedOnboarded, BigDecimal expectedReservation) {
        final InitiativeCounters result = initInitiativeCounterService.initCounters(initiative).block();

        Assertions.assertNotNull(result);
        Assertions.assertSame(initiative.getInitiativeId(), result.getId());
        Assertions.assertSame(initiative.getInitiativeBudget(), result.getInitiativeBudget());

        checkCounters(result, expectedOnboarded, expectedReservation);

        Mockito.verify(initiativeCountersRepositoryMock).save(Mockito.same(result));
    }

    private void checkCounters(InitiativeCounters result, Long expectedOnboarded, BigDecimal expectedReservation) {
        Assertions.assertEquals(expectedOnboarded, result.getOnboarded());
        TestUtils.assertBigDecimalEquals(expectedReservation, result.getReservedInitiativeBudget());
    }

}
