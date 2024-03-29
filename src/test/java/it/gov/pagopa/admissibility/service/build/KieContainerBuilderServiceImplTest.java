package it.gov.pagopa.admissibility.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class KieContainerBuilderServiceImplTest {

    @BeforeAll
    public static void configDroolsLogs() {
        ((Logger)LoggerFactory.getLogger("org.kie.api.internal.utils")).setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("org.drools")).setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.DEBUG);
    }

    @Test
    void buildAllNotFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.empty());
        // When
        KieBase result = kieContainerBuilderService.buildAll().block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }

    @Test
    void buildAllFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        DroolsRule dr = new DroolsRule();
        dr.setId("InitiativeID");
        dr.setName("name");
        String ruleCondition = "eval(true)";
        String ruleConsequence = "System.out.println(\"EXECUTED\");";
        dr.setRule(ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(dr.getId(), dr.getName(), ruleCondition, ruleConsequence));

        Flux<DroolsRule> rulesFlux = Flux.just(dr);
        Mockito.when(droolsRuleRepository.findAll()).thenReturn(rulesFlux);
        // When
        KieBase result = kieContainerBuilderService.buildAll().block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }
}