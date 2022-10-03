package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRule;
import it.gov.pagopa.admissibility.service.build.InitInitiativeCounterService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class BeneficiaryRuleBuilderMediatorServiceTest {

    // mocks
    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRuleMock;
    private final DroolsRuleRepository droolsRuleRepositoryMock;
    private final InitInitiativeCounterService initInitiativeCounterServiceMock;
    private final KieContainerBuilderService kieContainerBuilderServiceMock;
    private final OnboardingContextHolderService onboardingContextHolderServiceMock;
    private final ErrorNotifierService errorNotifierServiceMock;

    private final KieContainer newKieContainerBuiltmock = Mockito.mock(KieContainer.class);

    // service
    private final BeneficiaryRuleBuilderMediatorService service;

    public BeneficiaryRuleBuilderMediatorServiceTest() {
        this.beneficiaryRule2DroolsRuleMock = Mockito.mock(BeneficiaryRule2DroolsRule.class);
        this.droolsRuleRepositoryMock = Mockito.mock(DroolsRuleRepository.class);
        this.initInitiativeCounterServiceMock = Mockito.mock(InitInitiativeCounterService.class);
        this.kieContainerBuilderServiceMock = Mockito.mock(KieContainerBuilderService.class);
        this.onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);
        this.errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);

        service = new BeneficiaryRuleBuilderMediatorServiceImpl("PT1S", beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock, kieContainerBuilderServiceMock, onboardingContextHolderServiceMock, initInitiativeCounterServiceMock, errorNotifierServiceMock, TestUtils.objectMapper);
    }

    @BeforeEach
    void configureMocks() {
        Mockito.when(beneficiaryRule2DroolsRuleMock.apply(Mockito.any())).thenAnswer(invocation -> {
            Initiative2BuildDTO i = invocation.getArgument(0);
            return new DroolsRule(i.getInitiativeId(), i.getInitiativeName(), "RULE",
                    InitiativeConfig.builder()
                            .initiativeId(i.getInitiativeId())
                            .startDate(i.getGeneral().getStartDate())
                            .endDate(i.getGeneral().getEndDate())
                            .pdndToken(i.getPdndToken())
                            .automatedCriteriaCodes(List.of("CODE"))
                            .initiativeBudget(i.getGeneral().getBudget())
                            .beneficiaryInitiativeBudget(i.getGeneral().getBeneficiaryBudget())
                            .build());
        });
        Mockito.when(droolsRuleRepositoryMock.save(Mockito.any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieContainerBuiltmock));
        Mockito.when(initInitiativeCounterServiceMock.initCounters(Mockito.any())).thenAnswer(i->Mono.just(i.getArgument(0)));
    }

    @Test
    void testSuccessful() {
        // given
        int N = 10;
        List<Initiative2BuildDTO> initiatives = IntStream.range(0, N).mapToObj(Initiative2BuildDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Message<String>> inputFlux = Flux.fromIterable(initiatives).map(TestUtils::jsonSerializer).map(MessageBuilder::withPayload).map(MessageBuilder::build);

        // when
        service.execute(inputFlux);

        // then
        Mockito.verify(beneficiaryRule2DroolsRuleMock, Mockito.times(N)).apply(Mockito.any());
        initiatives.forEach(i -> {
            Mockito.verify(beneficiaryRule2DroolsRuleMock).apply(i);
            Mockito.verify(droolsRuleRepositoryMock).save(Mockito.argThat(dr -> dr.getId().equals(i.getInitiativeId())));
        });
        Mockito.verify(kieContainerBuilderServiceMock, Mockito.atLeast(1)).buildAll();
        Mockito.verify(onboardingContextHolderServiceMock, Mockito.atLeast(1)).setBeneficiaryRulesKieContainer(Mockito.same(newKieContainerBuiltmock));

        Mockito.verifyNoInteractions(errorNotifierServiceMock);
    }
}
