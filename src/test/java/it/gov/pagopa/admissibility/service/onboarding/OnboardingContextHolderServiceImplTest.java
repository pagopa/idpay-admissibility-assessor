package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.AESTokenService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class OnboardingContextHolderServiceImplTest {

    @Mock private KieContainerBuilderService kieContainerBuilderServiceMock;
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private ApplicationEventPublisher applicationEventPublisherMock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplateMock;
    @Mock private AESTokenService aesTokenServiceMock;
    private OnboardingContextHolderService onboardingContextHolderService;

    private final KieBase expectedKieBase = new KieContainerBuilderServiceImpl(droolsRuleRepositoryMock).build(Flux.empty()).block();

    private Field apiKeysCacheField;

    void init(boolean isRedisCacheEnabled){
        Assertions.assertNotNull(expectedKieBase);

        Mockito.when(droolsRuleRepositoryMock.findAll()).thenReturn(Flux.empty());
        Mockito.when(kieContainerBuilderServiceMock.build(Mockito.any())).thenReturn(Mono.just(expectedKieBase));

        if (isRedisCacheEnabled) {
            byte[] expectedKieBaseSerialized = SerializationUtils.serialize(expectedKieBase);
            Assertions.assertNotNull(expectedKieBaseSerialized);
            Mockito.when(reactiveRedisTemplateMock.opsForValue().get(Mockito.anyString())).thenReturn(Mono.just(expectedKieBaseSerialized));
        }

        apiKeysCacheField = ReflectionUtils.findField(OnboardingContextHolderServiceImpl.class, "apiKeysPDNDConcurrentMap");
        Assertions.assertNotNull(apiKeysCacheField);
        ReflectionUtils.makeAccessible(apiKeysCacheField);

        onboardingContextHolderService = new OnboardingContextHolderServiceImpl(kieContainerBuilderServiceMock, droolsRuleRepositoryMock, applicationEventPublisherMock, reactiveRedisTemplateMock, aesTokenServiceMock, isRedisCacheEnabled, true);
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
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
                .initiativeName("NAME")
                .initiativeBudget(BigDecimal.valueOf(100))
                .status("STATUS")
                .automatedCriteria(new ArrayList<>())
                .automatedCriteriaCodes(List.of("CODE1"))
                .apiKeyClientId("PDND-API-KEY-CLIENT-ID")
                .apiKeyClientAssertion("PDND-KEY-CLIENT-ASSERTION")
                .organizationId("ORGANIZATION-ID")
                .organizationName("ORGANIZATIONNAME")
                .startDate(LocalDate.MIN)
                .rankingInitiative(Boolean.TRUE)
                .rankingFields(List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build()))
                .initiativeRewardType("REFUND")
                .isLogoPresent(Boolean.FALSE)
                .build();


        // When
        onboardingContextHolderService.setInitiativeConfig(initiativeConfig);
        InitiativeConfig result = onboardingContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertSame(initiativeConfig, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testGetApiKeysPDND(boolean isRedisCacheEnabled){
        // Given
        init(isRedisCacheEnabled);

        InitiativeConfig initiativeConfig = getInitiativeConfig();

        Map<String, ApiKeysPDND> apiKeysPDNDMapMock = new ConcurrentHashMap<>();

        AgidJwtTokenPayload agidJwtTokenPayloadMock = AgidJwtTokenPayload.builder()
                .iss("ISS1")
                .sub("SUB1")
                .aud("AUD1")
                .build();
        apiKeysPDNDMapMock.put(initiativeConfig.getInitiativeId(),
                ApiKeysPDND.builder()
                        .apiKeyClientId(initiativeConfig.getApiKeyClientId())
                        .apiKeyClientAssertion(initiativeConfig.getApiKeyClientAssertion())
                        .agidJwtTokenPayload(agidJwtTokenPayloadMock)
                        .build());

        ReflectionUtils.setField(apiKeysCacheField, onboardingContextHolderService, apiKeysPDNDMapMock);

        // When
        ApiKeysPDND result = onboardingContextHolderService.getPDNDapiKeys(initiativeConfig);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeConfig.getApiKeyClientId(), result.getApiKeyClientId());
        Assertions.assertEquals(initiativeConfig.getApiKeyClientAssertion(), result.getApiKeyClientAssertion());
        assertionAgidPayloadTokenFields(result);
        Mockito.verify(aesTokenServiceMock, Mockito.never()).decrypt(Mockito.anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testGetApiKeysPDNDNotInCache(boolean isRedisCacheEnabled){
        // Given
        init(isRedisCacheEnabled);
        InitiativeConfig initiativeConfig = getInitiativeConfig();

        Mockito.when(aesTokenServiceMock.decrypt(Mockito.anyString())).thenAnswer(i -> i.getArguments()[0]);

        // When
        ApiKeysPDND result = onboardingContextHolderService.getPDNDapiKeys(initiativeConfig);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeConfig.getApiKeyClientId(), result.getApiKeyClientId());
        Assertions.assertEquals(initiativeConfig.getApiKeyClientAssertion(), result.getApiKeyClientAssertion());
        assertionAgidPayloadTokenFields(result);
        Mockito.verify(aesTokenServiceMock, Mockito.times(2)).decrypt(Mockito.anyString());

        Map<String, ApiKeysPDND> apiKeysCacheValue = getApiKeysCache(apiKeysCacheField);
        ApiKeysPDND apiKeysPdndInCache = apiKeysCacheValue.get(initiativeConfig.getInitiativeId());
        assertionAgidPayloadTokenFields(apiKeysPdndInCache);

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSetPDNDapiKeysInitiativeWithAutomatedCriteria(boolean isRedisCacheEnabled){
        // Given
        init(isRedisCacheEnabled);

        InitiativeConfig initiativeConfig = getInitiativeConfig();

        Mockito.when(aesTokenServiceMock.decrypt(Mockito.anyString())).thenAnswer(i -> i.getArguments()[0]);

        // When
        onboardingContextHolderService.setPDNDapiKeys(initiativeConfig);

        // Then
        Map<String, ApiKeysPDND> apiKeysCacheValue = getApiKeysCache(apiKeysCacheField);
        Assertions.assertTrue(apiKeysCacheValue.containsKey(initiativeConfig.getInitiativeId()));
        ApiKeysPDND apiKeysPdndInCache = apiKeysCacheValue.get(initiativeConfig.getInitiativeId());
        assertionAgidPayloadTokenFields(apiKeysPdndInCache);
    }

    private void assertionAgidPayloadTokenFields(ApiKeysPDND apiKeysPdndInCache) {
        TestUtils.checkNotNullFields(apiKeysPdndInCache);

        Assertions.assertEquals("SUB1", apiKeysPdndInCache.getAgidJwtTokenPayload().getSub());
        Assertions.assertEquals("ISS1", apiKeysPdndInCache.getAgidJwtTokenPayload().getIss());
        Assertions.assertEquals("AUD1", apiKeysPdndInCache.getAgidJwtTokenPayload().getAud());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSetPDNDapiKeysInitiativeWithoutAutomatedCriteria(boolean isRedisCacheEnabled){
        // Given
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID-NEW";
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
                .initiativeName("NAME")
                .initiativeBudget(BigDecimal.valueOf(100))
                .status("STATUS")
                .automatedCriteriaCodes(List.of("CODE1"))
                .organizationId("ORGANIZATION-ID")
                .startDate(LocalDate.MIN)
                .rankingInitiative(Boolean.FALSE)
                .build();

        // When
        onboardingContextHolderService.setPDNDapiKeys(initiativeConfig);

        // Then
        Map<String, ApiKeysPDND> apiKeysCacheValue = getApiKeysCache(apiKeysCacheField);
        Assertions.assertEquals(0,apiKeysCacheValue.size());
        Assertions.assertFalse(apiKeysCacheValue.containsKey(initiativeId));
    }

    private InitiativeConfig getInitiativeConfig() {
        return InitiativeConfig.builder()
                .initiativeId("INITIATIVE-ID")
                .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
                .initiativeName("NAME")
                .initiativeBudget(BigDecimal.valueOf(100))
                .status("STATUS")
                .automatedCriteriaCodes(List.of("CODE1"))
                .apiKeyClientId(Initiative2BuildDTOFaker.getStringB64("apiKeyClientId"))
                .apiKeyClientAssertion(Initiative2BuildDTOFaker.getClientAssertion(
                        "apiKeyClientAssertionFirstElement",
                        Initiative2BuildDTOFaker.getAgidTokenPayload(String.valueOf(1)),
                        "apiKeyClientAssertionLastElement"))
                .organizationId("ORGANIZATION-ID")
                .startDate(LocalDate.MIN)
                .rankingInitiative(Boolean.TRUE)
                .rankingFields(List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build()))
                .build();
    }

    private Map<String, ApiKeysPDND> getApiKeysCache(Field apiKeysCacheField){
        Object cache = ReflectionUtils.getField(apiKeysCacheField, onboardingContextHolderService);
        Assertions.assertNotNull(cache);
        //noinspection unchecked
        return (Map<String, ApiKeysPDND>) cache;
    }
}