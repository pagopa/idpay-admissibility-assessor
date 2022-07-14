package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.mapper.InitiativeDTO2ConfigMapper;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.rest.initiative.InitiativeRestService;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.drools.KieContainerBuilderService;
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
    private final InitiativeRestService initiativeRestService;
    private final InitiativeDTO2ConfigMapper initiativeDTO2ConfigMapper;
    private final CriteriaCodeService criteriaCodeService;

    private KieContainer kieContainer;
    private final Map<String, InitiativeConfig> initiativeId2Config=new HashMap<>();


    public OnboardingContextHolderServiceImpl(InitiativeRestService initiativeRestService, KieContainerBuilderService kieContainerBuilderService, CriteriaCodeService criteriaCodeService, InitiativeDTO2ConfigMapper initiativeDTO2ConfigMapper) {
        this.initiativeRestService = initiativeRestService;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.initiativeDTO2ConfigMapper = initiativeDTO2ConfigMapper;
        this.criteriaCodeService = criteriaCodeService;
        refreshKieContainer();
    }

    //region kieContainer holder
    @Override
    public void setKieContainer(KieContainer kiecontainer) {
        this.kieContainer=kiecontainer; //TODO store in cache

    }

    @Override
    public KieContainer getKieContainer() {
        return kieContainer;
    }

    // TODO use cache
    @Scheduled(fixedRateString = "${app.rules.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        // TODO use the KieContainerBuilderService
        kieContainer = kieContainerBuilderService.buildAll();
    }
    //endregion

    //region initiativeConfig holder
    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        return initiativeId2Config.computeIfAbsent(initiativeId, this::retrieveInitiativeConfig);
    }
    // TODO read from cache
    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        InitiativeDTO initiative = initiativeRestService.findById(initiativeId).block();
        if (initiative==null){
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return initiativeDTO2ConfigMapper.apply(initiative);
    }
    //endregion

    //region criteriaCode holder
    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {
        return criteriaCodeService.getCriteriaCodeConfig(criteriaCode);
    }
    //endregion
}
