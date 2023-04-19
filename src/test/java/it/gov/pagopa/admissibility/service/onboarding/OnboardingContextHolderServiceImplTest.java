package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.api.KieBase;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OnboardingContextHolderServiceImplTest {

    @Mock private KieContainerBuilderService kieContainerBuilderServiceMock;
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private ApplicationEventPublisher applicationEventPublisherMock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplateMock;
    private OnboardingContextHolderService onboardingContextHolderService;

    private final KieBase expectedKieBase = new KieContainerBuilderServiceImpl(droolsRuleRepositoryMock).build(Flux.empty()).block();

    void init(boolean isRedisCacheEnabled){
        Assertions.assertNotNull(expectedKieBase);

        Mockito.when(droolsRuleRepositoryMock.findAll()).thenReturn(Flux.empty());
        Mockito.when(kieContainerBuilderServiceMock.build(Mockito.any())).thenReturn(Mono.just(expectedKieBase));

        if (isRedisCacheEnabled) {
            byte[] expectedKieBaseSerialized = SerializationUtils.serialize(expectedKieBase);
            Assertions.assertNotNull(expectedKieBaseSerialized);
            Mockito.when(reactiveRedisTemplateMock.opsForValue().get(Mockito.anyString())).thenReturn(Mono.just(expectedKieBaseSerialized));
        }

        onboardingContextHolderService = new OnboardingContextHolderServiceImpl(kieContainerBuilderServiceMock, droolsRuleRepositoryMock, applicationEventPublisherMock, reactiveRedisTemplateMock, isRedisCacheEnabled, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getKieContainer(boolean isRedisCacheEnabled) {
        // Given
        init(isRedisCacheEnabled);

        // When
        KieBase result = onboardingContextHolderService.getBeneficiaryRulesKieBase();

        //Then
        Assertions.assertNotNull(result);
        if (!isRedisCacheEnabled) {
            Assertions.assertSame(expectedKieBase, result);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNotRetrieveInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.empty());

        // When
        InitiativeConfig result = onboardingContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testRetrieveInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        DroolsRule droolsRule = DroolsRule.builder().initiativeConfig(initiativeConfig).build();
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.just(droolsRule));

        // When
        InitiativeConfig result = onboardingContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSetInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
                .initiativeName("NAME")
                .initiativeBudget(BigDecimal.valueOf(100))
                .status("STATUS")
                .automatedCriteria(new ArrayList<>())
                .automatedCriteriaCodes(List.of("CODE1"))
                .pdndToken("PDND-TOKEN")
                .organizationId("ORGANIZATION-ID")
                .startDate(LocalDate.MIN)
                .rankingInitiative(Boolean.TRUE)
                .rankingFields(List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build()))
                .initiativeRewardType("REFUND")
                .organizationName("ORGANIZATIONNAME")
                .build();


        // When
        onboardingContextHolderService.setInitiativeConfig(initiativeConfig);
        InitiativeConfig result = onboardingContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
    }
}