package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BeneficiaryRuleMediatorServiceTest {

    // mocks
    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRuleMock;
    private final DroolsRuleRepository droolsRuleRepositoryMock;
    private final KieContainerBuilderService kieContainerBuilderServiceMock;
    private final OnboardingContextHolderService onboardingContextHolderServiceMock;

    private final KieContainer newKieContainerBuiltmock = Mockito.mock(KieContainer.class);

    // service
    private final BeneficiaryRuleMediatorService service;

    public BeneficiaryRuleMediatorServiceTest() {
        this.beneficiaryRule2DroolsRuleMock = Mockito.mock(BeneficiaryRule2DroolsRule.class);
        this.droolsRuleRepositoryMock = Mockito.mock(DroolsRuleRepository.class);
        this.kieContainerBuilderServiceMock = Mockito.mock(KieContainerBuilderService.class);
        this.onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);

        service = new BeneficiaryRuleMediatorServiceImpl("PT1S", beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock, kieContainerBuilderServiceMock, onboardingContextHolderServiceMock);
    }

    @BeforeEach
    public void configureMocks(){
        Mockito.when(beneficiaryRule2DroolsRuleMock.apply(Mockito.any())).thenAnswer(invocation-> {
            Initiative2BuildDTO i = invocation.getArgument(0);
            return new DroolsRule(i.getInitiativeId(), i.getInitiativeName(), "RULE",
                    new InitiativeConfig(i.getInitiativeId(),i.getGeneral().getStartDate(),i.getGeneral().getEndDate(),
                            i.getPdndToken(), List.of("CODE"),i.getGeneral().getBudget(),
                            i.getGeneral().getBeneficiaryBudget()));
        });
        Mockito.when(droolsRuleRepositoryMock.save(Mockito.any())).thenAnswer(invocation-> Mono.just(invocation.getArgument(0)));
        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieContainerBuiltmock));
    }

    @Test
    public void testSuccessful(){
        // given
        int N=10;
        List<Initiative2BuildDTO> initiatives = IntStream.range(0,N).mapToObj(Initiative2BuildDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Initiative2BuildDTO> inputFlux = Flux.fromIterable(initiatives);

        // when
        service.execute(inputFlux);

        // then
        Mockito.verify(beneficiaryRule2DroolsRuleMock, Mockito.times(N)).apply(Mockito.any());
        initiatives.forEach(i-> {
            Mockito.verify(beneficiaryRule2DroolsRuleMock).apply(Mockito.same(i));
            Mockito.verify(droolsRuleRepositoryMock).save(Mockito.argThat(dr -> dr.getId().equals(i.getInitiativeId())));
        });
        Mockito.verify(kieContainerBuilderServiceMock, Mockito.atLeast(1)).buildAll();
        Mockito.verify(onboardingContextHolderServiceMock, Mockito.atLeast(1)).setBeneficiaryRulesKieContainer(Mockito.same(newKieContainerBuiltmock));
    }
}
