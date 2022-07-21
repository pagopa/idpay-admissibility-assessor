package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import org.kie.api.runtime.KieContainer;


/**
 * This component will retrieve the beneficiaries' rules kieContainer and the PDND token associated to the input initiative id
 * It will also cache the new kieContainer created
 * */
public interface OnboardingContextHolderService {
    KieContainer getBeneficiaryRulesKieContainer();
    void setBeneficiaryRulesKieContainer(KieContainer kieContainer);

    InitiativeConfig getInitiativeConfig(String initiativeId);
}
