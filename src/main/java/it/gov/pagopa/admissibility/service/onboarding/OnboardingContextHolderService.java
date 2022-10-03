package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.kie.api.runtime.KieContainer;


/**
 * This component will retrieve the beneficiaries' rules kieContainer and the PDND token associated to the input initiative id
 * It will also update the cached version when new rules arrives
 * */
public interface OnboardingContextHolderService {
    KieContainer getBeneficiaryRulesKieContainer();
    void setBeneficiaryRulesKieContainer(KieContainer kieContainer);

    InitiativeConfig getInitiativeConfig(String initiativeId);

    void setInitiativeConfig(InitiativeConfig initiativeConfig);
}
