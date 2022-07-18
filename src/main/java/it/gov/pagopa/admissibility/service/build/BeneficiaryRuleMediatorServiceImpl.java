package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class BeneficiaryRuleMediatorServiceImpl implements BeneficiaryRuleMediatorService {

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final OnboardingContextHolderService onboardingContextHolderService;

    public BeneficiaryRuleMediatorServiceImpl(BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, OnboardingContextHolderService onboardingContextHolderService) {
        this.beneficiaryRule2DroolsRule = beneficiaryRule2DroolsRule;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public void execute(Flux<Initiative2BuildDTO> initiativeBeneficiaryRuleDTOFlux) {
        beneficiaryRule2DroolsRule.apply(initiativeBeneficiaryRuleDTOFlux) // TODO handle null value due to invalid ruleit.gov.pagopa.admissibility.service.build.BeneficiaryRuleMediatorService
                .flatMap(droolsRuleRepository::save)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(onboardingContextHolderService::setKieContainer);
    }
}
