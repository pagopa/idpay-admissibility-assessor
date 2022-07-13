package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.initiative.InitiativeRestService;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl implements OnboardingContextHolderService {

    private final KieServices kieServices = KieServices.Factory.get();

    private final InitiativeRestService initiativeRestService;

    private KieContainer kieContainer;

    private Map<String, InitiativeConfig> initiativeId2Config;


    public OnboardingContextHolderServiceImpl(InitiativeRestService initiativeRestService) {
        this.initiativeRestService = initiativeRestService;
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
        InitiativeDTO initiative = initiativeRestService.findById(initiativeId);
        if (initiative==null){
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        InitiativeConfig out = new InitiativeConfig();
        //TODO fill the out variable using initiative

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
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules.drl"));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
