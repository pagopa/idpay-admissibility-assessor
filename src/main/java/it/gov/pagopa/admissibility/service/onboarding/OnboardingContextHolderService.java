package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import org.kie.api.runtime.KieContainer;


/**
 * This component will retrieve the KieContainer configured with all the initiatives and the PDND token associated to the input initiative id
 * it will also cache the new container created
 * */
public interface OnboardingContextHolderService {
    KieContainer getKieContainer();

    InitiativeConfig getInitiativeConfig(String initiativeId);

    void setKieContainer(KieContainer kieContainer);
}
