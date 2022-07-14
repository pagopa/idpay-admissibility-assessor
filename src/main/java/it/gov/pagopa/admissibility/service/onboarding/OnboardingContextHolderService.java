package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.kie.api.runtime.KieContainer;


/**
 * This component will retrieve the KieContainer configured with all the initiatives and the PDND token associated to the input initiative id
 * it will also cache the new container created
 * */
public interface OnboardingContextHolderService {
    KieContainer getKieContainer();
    void setKieContainer(KieContainer kieContainer);

    InitiativeConfig getInitiativeConfig(String initiativeId);
    CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode);

}
