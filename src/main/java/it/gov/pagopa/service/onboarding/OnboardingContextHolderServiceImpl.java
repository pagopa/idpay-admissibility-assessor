package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.InitiativeConfig;
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

    private KieContainer kieContainer;

    private Map<String, InitiativeConfig> initiativeId2Config;

    public OnboardingContextHolderServiceImpl() {
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

    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        InitiativeConfig out = new InitiativeConfig();
        out.setInitiativeId(initiativeId);
        out.setAutomatedCriteriaCodes(Collections.singletonList("CriteriaCode1"));
        return out;  // TODO
    }

    // TODO use cache
    @Scheduled(fixedRateString = "${app.rules.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        // TODO access to DB read rules
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules.drl"));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
