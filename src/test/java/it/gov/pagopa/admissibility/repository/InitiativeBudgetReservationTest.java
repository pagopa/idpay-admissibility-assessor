package it.gov.pagopa.admissibility.repository;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import java.math.BigDecimal;

public class InitiativeBudgetReservationTest extends BaseIntegrationTest {

    @Autowired
    private DroolsRuleRepository droolsRuleRepository;
    @Autowired
    private InitiativeBudgetReservation initiativeBudgetReservation;

    @Test
    public void testReservation(){
        droolsRuleRepository.save(DroolsRule.builder()
                        .id("prova")
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeBudget(BigDecimal.valueOf(10000))
                        .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                        .build())
                .build()).block();
        System.out.println(initiativeBudgetReservation.reserveBudget("prova").block());
    }
}
