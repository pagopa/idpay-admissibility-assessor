package it.gov.pagopa.admissibility.service.drools;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

class KieContainerBuilderServiceImplTest {

    @Test
    void buildAllNotFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.empty());
        // When
        KieContainer result = kieContainerBuilderService.buildAll();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }

    @Test
    void buildAllFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        Flux<DroolsRule> rulesFlux = Flux.just(new DroolsRule("InitiativeID","InitiativeID","Condition","Consequence"));
        Mockito.when(droolsRuleRepository.findAll()).thenReturn(rulesFlux);
        // When
        KieContainer result = kieContainerBuilderService.buildAll();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }
}