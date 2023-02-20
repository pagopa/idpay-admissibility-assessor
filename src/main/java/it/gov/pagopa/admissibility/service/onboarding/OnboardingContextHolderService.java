package it.gov.pagopa.admissibility.service.onboarding;

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

    /** @deprecated use the {@link #getInitiativeConfig(String)} instead of this, which will call blocking logic */
    @Deprecated(forRemoval = true)
    InitiativeConfig getInitiativeConfigBlocking(String initiativeId);
    Mono<InitiativeConfig> getInitiativeConfig(String initiativeId);

    void setInitiativeConfig(InitiativeConfig initiativeConfig);
}
