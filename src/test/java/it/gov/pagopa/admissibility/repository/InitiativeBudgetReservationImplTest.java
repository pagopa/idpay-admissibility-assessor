package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InitiativeBudgetReservationImplTest extends BaseIntegrationTest {

    @Autowired
    private InitiativeCountersRepository initiativeCountersRepository;
    @Autowired
    private InitiativeBudgetReservationImpl initiativeBudgetReservationImpl;

    @Test
    public void testReservation() {
        int N = 1000;

        final BigDecimal budget = BigDecimal.valueOf(10099);
        final BigDecimal budgetReservedPerRequest = BigDecimal.valueOf(100);
        final BigDecimal expectedBudgetReserved = BigDecimal.valueOf(10000);
        final int expectedReservations = 100;

        storeInitiative(budget, budgetReservedPerRequest);

        final ExecutorService executorService = Executors.newFixedThreadPool(N);

        final List<Future<InitiativeCounters>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executorService.submit(() -> initiativeBudgetReservationImpl.reserveBudget("prova", budgetReservedPerRequest).block()))
                .collect(Collectors.toList());

        final long successfulReservation = tasks.stream().filter(t -> {
            try {
                return t.get() != null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).count();

        checkStoredBudgetReservation(expectedBudgetReserved, expectedReservations);
        Assertions.assertEquals(expectedReservations, successfulReservation);
    }

    private void storeInitiative(BigDecimal budget, BigDecimal reservedPerRequest) {
        initiativeCountersRepository.save(InitiativeCounters.builder()
                .id("prova")
                .initiativeBudget(budget)
                .build()).block();
    }

    private void checkStoredBudgetReservation(BigDecimal expectedBudgetReserved, int expectedReservations) {
        final InitiativeCounters c = initiativeCountersRepository.findById("prova").block();

        Assertions.assertNotNull(c);
        TestUtils.assertBigDecimalEquals(expectedBudgetReserved, c.getReservedInitiativeBudget());
        Assertions.assertEquals(expectedReservations, c.getOnboarded());
    }
}
