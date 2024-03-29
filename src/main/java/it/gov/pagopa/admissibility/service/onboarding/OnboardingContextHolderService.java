package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.kie.api.KieBase;
import reactor.core.publisher.Mono;

import java.util.Set;


/**
 * This component will retrieve the beneficiaries' rules kieContainer and the PDND token associated to the input initiative id
 * It will also update the cached version when new rules arrives
 * */
public interface OnboardingContextHolderService {
    KieBase getBeneficiaryRulesKieBase();
    Set<String> getBeneficiaryRulesKieInitiativeIds();
    void setBeneficiaryRulesKieBase(KieBase kieBase);

    Mono<InitiativeConfig> getInitiativeConfig(String initiativeId);
    void setInitiativeConfig(InitiativeConfig initiativeConfig);
    Mono<KieBase> refreshKieContainerCacheMiss();

}
