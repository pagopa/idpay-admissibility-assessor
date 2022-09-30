package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl implements OnboardingContextHolderService {

    private final KieContainerBuilderService kieContainerBuilderService;
    private final DroolsRuleRepository droolsRuleRepository;

    private KieContainer kieContainer;
    private final Map<String, InitiativeConfig> initiativeId2Config=new ConcurrentHashMap<>();

    public OnboardingContextHolderServiceImpl(KieContainerBuilderService kieContainerBuilderService, DroolsRuleRepository droolsRuleRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        refreshKieContainer(x -> applicationEventPublisher.publishEvent(new OnboardingContextHolderReadyEvent(this)));
    }

    public static class OnboardingContextHolderReadyEvent extends ApplicationEvent {
        public OnboardingContextHolderReadyEvent(Object source) {
            super(source);
        }
    }

    //region kieContainer holder
    @Override
    public void setBeneficiaryRulesKieContainer(KieContainer kiecontainer) {
        this.kieContainer=kiecontainer; //TODO store in cache

    }

    @Override
    public KieContainer getBeneficiaryRulesKieContainer() {
        return kieContainer;
    }

    // TODO use cache
    @Scheduled(initialDelayString = "${app.beneficiary-rule.cache.refresh-ms-rate}", fixedRateString = "${app.beneficiary-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"));
    }

    //TODO use cache
    public void refreshKieContainer(Consumer<? super KieContainer> subscriber){
        log.trace("[BENEFICIARY_RULE_BUILDER] Refreshing KieContainer");
        final Flux<DroolsRule> droolsRuleFlux = droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig()));

        kieContainerBuilderService.build(droolsRuleFlux).doOnNext(this::setBeneficiaryRulesKieContainer).subscribe(subscriber);
    }
    //endregion

    //region initiativeConfig holder
    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        return initiativeId2Config.computeIfAbsent(initiativeId, this::retrieveInitiativeConfig);
    }

    @Override
    public void setInitiativeConfig(InitiativeConfig initiativeConfig) {  //TODO save inside cache
        initiativeId2Config.put(initiativeConfig.getInitiativeId(),initiativeConfig);

    }

    // TODO read from cache
    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        DroolsRule droolsRule = droolsRuleRepository.findById(initiativeId).block();
        if (droolsRule==null){
            log.error("[ONBOARDING_CONTEXT] cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion
}
