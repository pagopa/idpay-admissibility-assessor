package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.common.mongo.MongoTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@MongoTest
class InitiativeCountersReservationOpsRepositoryImplTest {

    @Autowired
    protected InitiativeCountersRepository initiativeCountersRepository;
    @Autowired
    private InitiativeCountersReservationOpsRepositoryImpl initiativeCountersReservationOpsRepositoryImpl;

    @Test
    void testReservation() {
        int N = 1000;

        final BigDecimal budget = BigDecimal.valueOf(10099);
        final BigDecimal budgetReservedPerRequest = BigDecimal.valueOf(100);
        final BigDecimal expectedBudgetReserved = BigDecimal.valueOf(10000);
        final BigDecimal expectedBudgetResidual = BigDecimal.valueOf(99);
        final int expectedReservations = 100;

        storeInitiative(budget);

        final ExecutorService executorService = Executors.newFixedThreadPool(N);

        final List<Future<InitiativeCounters>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executorService.submit(() -> initiativeCountersReservationOpsRepositoryImpl.reserveBudget("prova", budgetReservedPerRequest).block()))
                .toList();

        final long successfulReservation = tasks.stream().filter(t -> {
            try {
                return t.get() != null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).count();

        checkStoredBudgetReservation(expectedBudgetReserved, expectedBudgetResidual, expectedReservations);
        Assertions.assertEquals(expectedReservations, successfulReservation);
    }

    private void storeInitiative(BigDecimal budget) {
        initiativeCountersRepository.save(InitiativeCounters.builder()
                .id("prova")
                .initiativeBudgetCents(euro2cents(budget))
                .residualInitiativeBudgetCents(euro2cents(budget))
                .build()).block();
    }

    private long euro2cents(BigDecimal budget) {
        return budget.longValue() * 100;
    }

    private void checkStoredBudgetReservation(BigDecimal expectedBudgetReservedCents, BigDecimal expectedResidualBudgetCents, int expectedReservations) {
        final InitiativeCounters c = initiativeCountersRepository.findById("prova").block();

        Assertions.assertNotNull(c);
        Assertions.assertEquals(euro2cents(expectedBudgetReservedCents), c.getReservedInitiativeBudgetCents());
        Assertions.assertEquals(euro2cents(expectedResidualBudgetCents), c.getResidualInitiativeBudgetCents());
        Assertions.assertEquals(expectedReservations, c.getOnboarded());
    }
}
