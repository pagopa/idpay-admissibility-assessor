package it.gov.pagopa.admissibility.service.onboarding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.AESTokenService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl implements OnboardingContextHolderService {

    private final KieContainerBuilderService kieContainerBuilderService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    private final Map<String, InitiativeConfig> initiativeId2Config=new ConcurrentHashMap<>();
    private final Map<String, ApiKeysPDND> apiKeysPDNDConcurrentMap = new ConcurrentHashMap<>();

    private final boolean isRedisCacheEnabled;
    private final AESTokenService aesTokenService;
    private final ObjectReader orAgidJwtTokenPayload;
    public static final String ONBOARDING_CONTEXT_HOLDER_CACHE_NAME = "beneficiary_rule";

    private KieBase kieBase;


    public OnboardingContextHolderServiceImpl(
            KieContainerBuilderService kieContainerBuilderService,
            DroolsRuleRepository droolsRuleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate,
            @Value("${spring.redis.enabled}") boolean isRedisCacheEnabled,
            AESTokenService aesTokenService,
            ObjectMapper objectMapper) {
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.isRedisCacheEnabled = isRedisCacheEnabled;
        this.aesTokenService = aesTokenService;
        this.orAgidJwtTokenPayload = objectMapper.readerFor(AgidJwtTokenPayload.class);
        refreshKieContainer(x -> applicationEventPublisher.publishEvent(new OnboardingContextHolderReadyEvent(this)));
    }

    public static class OnboardingContextHolderReadyEvent extends ApplicationEvent {
        public OnboardingContextHolderReadyEvent(Object source) {
            super(source);
        }
    }

    //region kieContainer holder
    @Override
    public void setBeneficiaryRulesKieBase(KieBase kieBase) {
        this.kieBase = kieBase;

        if (isRedisCacheEnabled) {
            byte[] kieBaseSerialized = SerializationUtils.serialize(kieBase);
            if (kieBaseSerialized != null) {
                reactiveRedisTemplate.opsForValue().set(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME, kieBaseSerialized).subscribe(x -> log.debug("Saving KieContainer in cache"));
            } else {
                reactiveRedisTemplate.delete(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME).subscribe(x -> log.debug("Clearing KieContainer in cache"));
            }
        }

    }

    @Override
    public KieBase getBeneficiaryRulesKieBase() {
        return kieBase;
    }

    @Scheduled(initialDelayString = "${app.beneficiary-rule.cache.refresh-ms-rate}", fixedRateString = "${app.beneficiary-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"));
    }

    public void refreshKieContainer(Consumer<? super KieBase> subscriber){
        if (isRedisCacheEnabled) {
            reactiveRedisTemplate.opsForValue().get(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME)
                    .map(c -> (KieBase) SerializationUtils.deserialize(c))
                    .doOnNext(c -> this.kieBase = c)
                    .switchIfEmpty(refreshKieContainerCacheMiss())
                    .subscribe(subscriber);
        } else {
            refreshKieContainerCacheMiss().subscribe(subscriber);
        }
    }

    private Mono<KieBase> refreshKieContainerCacheMiss() {
        log.trace("[BENEFICIARY_RULE_BUILDER] Refreshing KieContainer");
        initiativeId2Config.clear();
        final Flux<DroolsRule> droolsRuleFlux = droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig()));
        return kieContainerBuilderService.build(droolsRuleFlux).doOnNext(this::setBeneficiaryRulesKieBase);
    }
    //endregion

    //region initiativeConfig holder
    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        return initiativeId2Config.computeIfAbsent(initiativeId, this::retrieveInitiativeConfig);
    }

    @Override
    public void setInitiativeConfig(InitiativeConfig initiativeConfig) {
        initiativeId2Config.put(initiativeConfig.getInitiativeId(),initiativeConfig);
    }


    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        log.debug("[CACHE_MISS] Cannot find locally initiativeId {}", initiativeId);
        long startTime = System.currentTimeMillis();
        DroolsRule droolsRule = droolsRuleRepository.findById(initiativeId).block(Duration.ofSeconds(10));
        log.info("[CACHE_MISS] [PERFORMANCE_LOG] Time spent fetching initiativeId: {} ms", System.currentTimeMillis() - startTime);
        if (droolsRule==null){
            log.error("[ONBOARDING_CONTEXT] cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion

    //region PDND Api Keys holder
    @Override
    public void setPDNDapiKeys(InitiativeConfig initiativeConfig) {
        if(initiativeConfig.getApiKeyClientId() != null && initiativeConfig.getApiKeyClientAssertion() != null) {
            apiKeysPDNDConcurrentMap.put(
                    initiativeConfig.getInitiativeId(),
                    getApiKeysPDND(initiativeConfig)
            );
        }
    }

    @Override
    public ApiKeysPDND getPDNDapiKeys(InitiativeConfig initiativeConfig) {
        return apiKeysPDNDConcurrentMap.computeIfAbsent(initiativeConfig.getInitiativeId(), initiativeId -> getApiKeysPDND(initiativeConfig));
    }

    private ApiKeysPDND getApiKeysPDND(InitiativeConfig initiativeConfig) {
        try {
            ApiKeysPDND apiKeysPDND = ApiKeysPDND.builder()
                    .apiKeyClientId(aesTokenService.decrypt(initiativeConfig.getApiKeyClientId()))
                    .apiKeyClientAssertion(aesTokenService.decrypt(initiativeConfig.getApiKeyClientAssertion()))
                    .build();
            apiKeysPDND.setAgidJwtTokenPayload(retrieveAgidTokenPayload(apiKeysPDND.getApiKeyClientAssertion()));

            return apiKeysPDND;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[Admissibility] Error retrieving fields for AgidJWTTokenPayload",e);
        }
    }

    private AgidJwtTokenPayload retrieveAgidTokenPayload(String clientAssertion) throws JsonProcessingException {
        String[] splitClientAssertion = clientAssertion.split("\\.");
        String tokenInfoFromClientAssertion = new String(Base64.getDecoder().decode(splitClientAssertion[1]));
        return orAgidJwtTokenPayload.readValue(tokenInfoFromClientAssertion);
    }
    //endregion
}
