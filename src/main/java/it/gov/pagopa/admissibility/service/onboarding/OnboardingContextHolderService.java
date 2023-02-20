package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.kie.api.KieBase;
import reactor.core.publisher.Mono;


/**
 * This component will retrieve the beneficiaries' rules kieContainer and the PDND token associated to the input initiative id
 * It will also update the cached version when new rules arrives
 * */
public interface OnboardingContextHolderService {
    KieBase getBeneficiaryRulesKieBase();
    void setBeneficiaryRulesKieBase(KieBase kieBase);

    Mono<InitiativeConfig> getInitiativeConfig(String initiativeId);
    void setInitiativeConfig(InitiativeConfig initiativeConfig);

    void setPDNDapiKeys(InitiativeConfig initiativeConfig);

    ApiKeysPDND getPDNDapiKeys(InitiativeConfig initiativeConfig);
}
