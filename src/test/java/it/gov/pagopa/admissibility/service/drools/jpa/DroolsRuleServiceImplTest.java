package it.gov.pagopa.admissibility.service.drools.jpa;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DroolsRuleServiceImplTest {

    @Test
    void saveNotNulDroolsRule() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        DroolsRuleService droolsRuleService = new DroolsRuleServiceImpl(droolsRuleRepository);
        DroolsRule droolsRule = Mockito.mock(DroolsRule.class);
        Mono<DroolsRule> returnSave = Mono.just(droolsRule);
        Mockito.when(droolsRuleRepository.save(Mockito.same(droolsRule))).thenReturn(returnSave);

        // When
        Mono<DroolsRule> result = droolsRuleService.save(droolsRule);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Boolean.TRUE, result.hasElement().block());
        Mockito.verify(droolsRuleRepository).save(Mockito.same(droolsRule));
    }

    @Test
    void saveNullDroolsRule() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        DroolsRuleService droolsRuleService = new DroolsRuleServiceImpl(droolsRuleRepository);

        // When
        Mono<DroolsRule> result = droolsRuleService.save(null);

        //Then
        Assertions.assertEquals(Boolean.FALSE, result.hasElement().block());
        Mockito.verify(droolsRuleRepository,Mockito.never()).save(Mockito.any(DroolsRule.class));
    }

    @Test
    void findAll(){
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        DroolsRuleService droolsRuleService = new DroolsRuleServiceImpl(droolsRuleRepository);
        DroolsRule droolsRuleMock1 = Mockito.mock(DroolsRule.class);
        DroolsRule droolsRuleMock2 = Mockito.mock(DroolsRule.class);
        Flux<DroolsRule> droolsRuleFlux = Flux.just(droolsRuleMock1,droolsRuleMock2);
        Mockito.when(droolsRuleRepository.findAll()).thenReturn(droolsRuleFlux);

        // When
        Flux<DroolsRule> result = droolsRuleService.findAll();

        // Then
        Assertions.assertEquals(Boolean.TRUE, result.hasElements().block());
        Mockito.verify(droolsRuleRepository).findAll();
    }

    @Test
    void findAllNotFindRules(){
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        DroolsRuleService droolsRuleService = new DroolsRuleServiceImpl(droolsRuleRepository);
        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.empty());

        // When
        Flux<DroolsRule> result = droolsRuleService.findAll();

        // Then
        Assertions.assertEquals(Boolean.FALSE, result.hasElements().block());
        Mockito.verify(droolsRuleRepository).findAll();
    }
}