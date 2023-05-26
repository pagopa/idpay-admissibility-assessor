package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.reactive.kafka.utils.KafkaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.api.KieBase;
import org.mockito.Mockito;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
    private final BeneficiaryRuleFilterService beneficiaryRuleFilterServiceMock;

    private final KieBase newKieBaseBuiltMock = Mockito.mock(KieBase.class);

    public BeneficiaryRuleBuilderMediatorServiceTest() {
        this.beneficiaryRule2DroolsRuleMock = Mockito.mock(BeneficiaryRule2DroolsRule.class);
        this.droolsRuleRepositoryMock = Mockito.mock(DroolsRuleRepository.class);
        this.initInitiativeCounterServiceMock = Mockito.mock(InitInitiativeCounterService.class);
        this.kieContainerBuilderServiceMock = Mockito.mock(KieContainerBuilderService.class);
        this.onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderService.class);
        this.errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        this.beneficiaryRuleFilterServiceMock = Mockito.mock(BeneficiaryRuleFilterService.class);
    }

    @BeforeEach
    void configureMocks() {
        Mockito.when(beneficiaryRule2DroolsRuleMock.apply(Mockito.any())).thenAnswer(invocation -> {
            Initiative2BuildDTO i = invocation.getArgument(0);
            return new DroolsRule(i.getInitiativeId(), i.getInitiativeName(), "RULE", "RULEVERSION",
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
        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieBaseBuiltMock));
        Mockito.when(initInitiativeCounterServiceMock.initCounters(Mockito.any())).thenAnswer(i->Mono.just(i.getArgument(0)));
    }

    @ParameterizedTest
    @ValueSource(longs = {800,1000,1010})
    void testSuccessful(long commitDelay){
        // Given
        int N = 10;
        List<Initiative2BuildDTO> initiatives = IntStream.range(0, N).mapToObj(Initiative2BuildDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Message<String>> inputFlux = Flux.fromIterable(initiatives)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION_ID, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        BeneficiaryRuleBuilderMediatorService service = new BeneficiaryRuleBuilderMediatorServiceImpl("appName", commitDelay,"PT1S", beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock, kieContainerBuilderServiceMock, onboardingContextHolderServiceMock, initInitiativeCounterServiceMock, errorNotifierServiceMock, beneficiaryRuleFilterServiceMock, TestUtils.objectMapper);

        Mockito.when(beneficiaryRuleFilterServiceMock.filter(Mockito.any())).thenReturn(true);

        // when
        service.execute(inputFlux);

        // then
        Mockito.verify(beneficiaryRule2DroolsRuleMock, Mockito.times(N)).apply(Mockito.any());
        initiatives.forEach(i -> {
            Mockito.verify(beneficiaryRule2DroolsRuleMock).apply(i);
            Mockito.verify(droolsRuleRepositoryMock).save(Mockito.argThat(dr -> dr.getId().equals(i.getInitiativeId())));
        });

        Mockito.verify(kieContainerBuilderServiceMock, Mockito.atLeast(1)).buildAll();
        Mockito.verify(onboardingContextHolderServiceMock, Mockito.atLeast(1)).setBeneficiaryRulesKieBase(Mockito.same(newKieBaseBuiltMock));
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
    }

    @Test
    void filterInitiativeTest(){
        // Given
        BeneficiaryRuleBuilderMediatorService service = new BeneficiaryRuleBuilderMediatorServiceImpl("appName", 1000,"PT1S", beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock, kieContainerBuilderServiceMock, onboardingContextHolderServiceMock, initInitiativeCounterServiceMock, errorNotifierServiceMock, beneficiaryRuleFilterServiceMock, TestUtils.objectMapper);

        Initiative2BuildDTO initiative1 = Initiative2BuildDTOFaker.mockInstance(1);
        Initiative2BuildDTO initiative2 = Initiative2BuildDTOFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(initiative1, initiative2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION_ID, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        Mockito.when(beneficiaryRuleFilterServiceMock.filter(initiative1)).thenReturn(true);
        Mockito.when(beneficiaryRuleFilterServiceMock.filter(initiative2)).thenReturn(false);

        // When
        service.execute(msgs);

        // Then
        Mockito.verify(beneficiaryRuleFilterServiceMock, Mockito.times(2)).filter(Mockito.any());

        Mockito.verify(beneficiaryRule2DroolsRuleMock).apply(Mockito.any());
        Mockito.verify(droolsRuleRepositoryMock).save(Mockito.any());

        Mockito.verify(kieContainerBuilderServiceMock).buildAll();
        Mockito.verify(onboardingContextHolderServiceMock, Mockito.atLeast(1)).setBeneficiaryRulesKieBase(Mockito.same(newKieBaseBuiltMock));
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        BeneficiaryRuleBuilderMediatorService service = new BeneficiaryRuleBuilderMediatorServiceImpl("appName", 1000,"PT1S", beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock, kieContainerBuilderServiceMock, onboardingContextHolderServiceMock, initInitiativeCounterServiceMock, errorNotifierServiceMock, beneficiaryRuleFilterServiceMock, TestUtils.objectMapper);

        Initiative2BuildDTO initiative1 = Initiative2BuildDTOFaker.mockInstance(1);
        Initiative2BuildDTO initiative2 = Initiative2BuildDTOFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(initiative1, initiative2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION_ID, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .doOnNext(m->m.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        service.execute(msgs);

        // Then
        Mockito.verify(kieContainerBuilderServiceMock).buildAll();
        Mockito.verify(onboardingContextHolderServiceMock, Mockito.atLeast(1)).setBeneficiaryRulesKieBase(Mockito.same(newKieBaseBuiltMock));

        Mockito.verifyNoInteractions(errorNotifierServiceMock, beneficiaryRuleFilterServiceMock, beneficiaryRule2DroolsRuleMock, droolsRuleRepositoryMock);
    }
}
