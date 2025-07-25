package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl extends ReadinessStateHealthIndicator implements OnboardingContextHolderService {

    public static final String ONBOARDING_CONTEXT_HOLDER_CACHE_NAME = "beneficiary_rule";

    private final KieContainerBuilderService kieContainerBuilderService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    private final Map<String, InitiativeConfig> initiativeId2Config = new ConcurrentHashMap<>();

    private final boolean isRedisCacheEnabled;
    private final boolean preLoadContainer;

    private KieBase kieBase;
    private Set<String> kieInitiatives = Collections.emptySet();
    private byte[] kieBaseSerialized;

    private boolean contextReady=false;

    public OnboardingContextHolderServiceImpl(
            ApplicationAvailability applicationAvailability,
            ApplicationContext appContext,
            KieContainerBuilderService kieContainerBuilderService,
            DroolsRuleRepository droolsRuleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate,
            @Value("${spring.redis.enabled}") boolean isRedisCacheEnabled,
            @Value("${app.beneficiary-rule.pre-load}") boolean preLoadContainer
    ) {
        super(applicationAvailability);

        this.kieContainerBuilderService = kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.isRedisCacheEnabled = isRedisCacheEnabled;
        this.preLoadContainer = preLoadContainer;

        refreshKieContainer(
                x -> {
                    contextReady=true;
                    applicationEventPublisher.publishEvent(new OnboardingContextHolderReadyEvent(this));
                },
                Retry.max(3),
                e -> {
                    log.error("[BENEFICIARY_CONTEXT_START] Cannot build Drools container! Shutdown application!");
                    SpringApplication.exit(appContext, () -> 503);
                    return Mono.error(e);
                });
    }

    public static class OnboardingContextHolderReadyEvent extends ApplicationEvent {
        public OnboardingContextHolderReadyEvent(Object source) {
            super(source);
        }
    }

    //region kieContainer holder
    @Override
    public void setBeneficiaryRulesKieBase(KieBase newKieBase) {
        acceptNewKieBase(newKieBase);

        if (isRedisCacheEnabled) {
            kieBaseSerialized = SerializationUtils.serialize(newKieBase);
            if (kieBaseSerialized != null) {
                reactiveRedisTemplate.opsForValue().set(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME, kieBaseSerialized).subscribe(x -> log.info("[BENEFICIARY_RULE_BUILDER] KieContainer build and stored in cache"));
            } else {
                reactiveRedisTemplate.delete(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME).subscribe(x -> log.info("[BENEFICIARY_RULE_BUILDER] KieContainer removed from the cache"));
            }
        }
    }

    private void acceptNewKieBase(KieBase newKieBase) {
        preLoadKieBase(newKieBase);
        this.kieBase = newKieBase;
        this.kieInitiatives = readKieInitiatives(newKieBase);
        log.info("[BENEFICIARY_RULE_CONTAINER_LOAD] Rule engine rules loaded: {}", kieInitiatives);
    }

    private Set<String> readKieInitiatives(KieBase kieBase) {
        if (kieBase == null) {
            return Collections.emptySet();
        } else {
            return kieBase.getKiePackages().stream()
                    .flatMap(p -> p.getRules().stream())
                    .map(r -> ((RuleImpl) r).getAgendaGroup())
                    //.map(r -> (String) r.getMetaData().get("agenda-group" /*test agendaGroup*/))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());


        }
    }

    private void preLoadKieBase(KieBase kieBase) {
        if (preLoadContainer) {
            kieContainerBuilderService.preLoadKieBase(kieBase);
        }
    }

    @Override
    public KieBase getBeneficiaryRulesKieBase() {
        return kieBase;
    }

    @Override
    public Set<String> getBeneficiaryRulesKieInitiativeIds() {
        return kieInitiatives;
    }

    @Scheduled(initialDelayString = "${app.beneficiary-rule.cache.refresh-ms-rate}", fixedRateString = "${app.beneficiary-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer() {
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"), Retry.max(3), Mono::error);
    }

    public void refreshKieContainer(
            Consumer<? super KieBase> subscriber,
            Retry retrier,
            Function<? super Throwable, ? extends Mono<? extends KieBase>> onErrorResumer) {
        if (isRedisCacheEnabled) {
            reactiveRedisTemplate.opsForValue().get(ONBOARDING_CONTEXT_HOLDER_CACHE_NAME)
                    .mapNotNull(c -> {
                        if (!Arrays.equals(c, kieBaseSerialized)) {
                            this.kieBaseSerialized = c;
                            try {
                                KieBase newKieBase = org.apache.commons.lang3.SerializationUtils.deserialize(c);
                                acceptNewKieBase(newKieBase);
                            } catch (Exception e) {
                                log.warn("[BENEFICIARY_RULE_BUILDER] Cached KieContainer cannot be executed! refreshing it!");
                                return null;
                            }
                        }
                        return this.kieBase;
                    })
                    .switchIfEmpty(refreshKieContainerCacheMiss())
                    .retryWhen(retrier)
                    .onErrorResume(onErrorResumer)
                    .subscribe(subscriber);
        } else {
            refreshKieContainerCacheMiss()
                    .retryWhen(retrier)
                    .onErrorResume(onErrorResumer)
                    .subscribe(subscriber);
        }
    }

    @Override
    public Mono<KieBase> refreshKieContainerCacheMiss() {
        final Flux<DroolsRule> droolsRuleFlux = Mono.defer(() -> {
            log.info("[BENEFICIARY_RULE_BUILDER] Refreshing KieContainer");
            initiativeId2Config.clear();
            return Mono.empty();
        }).thenMany(droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig())));
        return kieContainerBuilderService.build(droolsRuleFlux).doOnNext(this::setBeneficiaryRulesKieBase);
    }
    //endregion

    //region initiativeConfig holder
    @Override
    public Mono<InitiativeConfig> getInitiativeConfig(String initiativeId) {
        InitiativeConfig cachedInitiativeConfig = initiativeId2Config.get(initiativeId);
        if (cachedInitiativeConfig == null) {
            return retrieveInitiativeConfig(initiativeId)
                    .doOnNext(initiativeConfig -> initiativeId2Config.put(initiativeId, initiativeConfig));
        } else {
            return Mono.just(cachedInitiativeConfig);
        }
    }

    @Override
    public void setInitiativeConfig(InitiativeConfig initiativeConfig) {
        initiativeId2Config.put(initiativeConfig.getInitiativeId(), initiativeConfig);
    }

    private Mono<InitiativeConfig> retrieveInitiativeConfig(String initiativeId) {
        log.debug("[CACHE_MISS] Cannot find locally initiativeId {}", initiativeId);
        long startTime = System.currentTimeMillis();
        return droolsRuleRepository.findById(initiativeId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("[ONBOARDING_CONTEXT] cannot find initiative having id %s".formatted(initiativeId));
                    return Mono.empty();
                }))
                .map(DroolsRule::getInitiativeConfig)
                .doFinally(x -> log.info("[CACHE_MISS] [PERFORMANCE_LOG] Time spent fetching initiativeId {} ({}): {} ms", initiativeId, x.toString(), System.currentTimeMillis() - startTime));
    }
    //endregion

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        return contextReady
                ? ReadinessState.ACCEPTING_TRAFFIC
                : ReadinessState.REFUSING_TRAFFIC;
    }

}
