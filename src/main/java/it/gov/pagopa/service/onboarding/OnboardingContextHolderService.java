package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.InitiativeConfig;
import org.kie.api.runtime.KieContainer;


/**
 * This component will retrieve the KieContainer configured with all the initiatives and the PDND token associated to the input initiative id
 * */
public interface OnboardingContextHolderService {
    KieContainer getKieContainer();

    InitiativeConfig getInitiativeConfig(String initiativeId);
}
