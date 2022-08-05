package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InitiativeBudgetReservationTest extends BaseIntegrationTest {

    @Autowired
    private DroolsRuleRepository droolsRuleRepository;
    @Autowired
    private InitiativeBudgetReservation initiativeBudgetReservation;

    @Test
    public void testReservation() {
        int N = 1000;

        final BigDecimal budget = BigDecimal.valueOf(10099);
        final BigDecimal budgetReservedPerRequest = BigDecimal.valueOf(100);
        final BigDecimal expectedBudgetReserved = BigDecimal.valueOf(10000);
        final int expectedReservations = 100;

        storeInitiative(budget, budgetReservedPerRequest);

        final ExecutorService executorService = Executors.newFixedThreadPool(N);

        final List<Future<DroolsRule>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executorService.submit(() -> initiativeBudgetReservation.reserveBudget("prova", budgetReservedPerRequest).block()))
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

        // check that a new save will not override the counters
        storeInitiative(budget, budgetReservedPerRequest);
        checkStoredBudgetReservation(expectedBudgetReserved, expectedReservations);
    }

    private void storeInitiative(BigDecimal budget, BigDecimal reservedPerRequest) {
        droolsRuleRepository.save(DroolsRule.builder()
                .id("prova")
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeBudget(budget)
                        .beneficiaryInitiativeBudget(reservedPerRequest)
                        .build())
                .build()).block();
    }

    private void checkStoredBudgetReservation(BigDecimal expectedBudgetReserved, int expectedReservations) {
        final DroolsRule dr = droolsRuleRepository.findById("prova").block();

        Assertions.assertNotNull(dr);
        TestUtils.assertBigDecimalEquals(expectedBudgetReserved, dr.getInitiativeConfig().getReservedInitiativeBudget());
        Assertions.assertEquals(expectedReservations, dr.getInitiativeConfig().getOnboarded());
    }
}
