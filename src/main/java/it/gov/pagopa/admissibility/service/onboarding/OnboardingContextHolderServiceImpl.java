package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.initiative.InitiativeRestService;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import it.gov.pagopa.admissibility.service.drools.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl implements OnboardingContextHolderService {
    private final InitiativeRestService initiativeRestService;
    private final KieContainerBuilderService kieContainerBuilderService;

    private KieContainer kieContainer;

    private Map<String, InitiativeConfig> initiativeId2Config;


    public OnboardingContextHolderServiceImpl(InitiativeRestService initiativeRestService, KieContainerBuilderService kieContainerBuilderService) {
        this.initiativeRestService = initiativeRestService;
        this.kieContainerBuilderService = kieContainerBuilderService;
        refreshKieContainer();
    }

    @Override
    public KieContainer getKieContainer() {
        return kieContainer;
    }

    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        return initiativeId2Config.computeIfAbsent(initiativeId, this::retrieveInitiativeConfig);
    }

    @Override
    public void setKieContainer(KieContainer kiecontainer) {
        this.kieContainer=kiecontainer; //TODO store in cache

    }

    // TODO read from cache
    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        InitiativeDTO initiative = initiativeRestService.findById(Mono.just(initiativeId)).block();
        if (initiative==null){
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        InitiativeConfig out = new InitiativeConfig();
        //TODO call the mapper

        //TODO remove these static setters
        out.setInitiativeId(initiativeId);
        out.setAutomatedCriteriaCodes(Collections.singletonList("CriteriaCode1"));
        return out;
    }

    // TODO use cache
    @Scheduled(fixedRateString = "${app.rules.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        // TODO use the KieContainerBuilderService
        kieContainer = kieContainerBuilderService.buildAll();
    }
}
