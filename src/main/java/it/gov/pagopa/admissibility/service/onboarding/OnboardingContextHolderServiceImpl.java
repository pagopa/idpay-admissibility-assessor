package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OnboardingContextHolderServiceImpl implements OnboardingContextHolderService {
    private final KieContainerBuilderService kieContainerBuilderService;
    private final CriteriaCodeService criteriaCodeService;

    private final DroolsRuleRepository droolsRuleRepository;

    private KieContainer kieContainer;
    private final Map<String, InitiativeConfig> initiativeId2Config=new HashMap<>();


    public OnboardingContextHolderServiceImpl(KieContainerBuilderService kieContainerBuilderService, CriteriaCodeService criteriaCodeService, DroolsRuleRepository droolsRuleRepository) {
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.criteriaCodeService = criteriaCodeService;
        this.droolsRuleRepository = droolsRuleRepository;
        refreshKieContainer();
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
    @Scheduled(fixedRateString = "${app.beneficiary-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        kieContainerBuilderService.buildAll().subscribe(this::setBeneficiaryRulesKieContainer);
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
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion
}
