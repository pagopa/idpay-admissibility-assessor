package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRule;
import it.gov.pagopa.admissibility.service.build.InitInitiativeCounterService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class BeneficiaryRuleBuilderMediatorServiceImpl implements BeneficiaryRuleBuilderMediatorService {

    private final Duration beneficiaryRulesBuildDelay;

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final OnboardingContextHolderService onboardingContextHolderService;
    private final InitInitiativeCounterService initInitiativeCounterService;

    public BeneficiaryRuleBuilderMediatorServiceImpl(@Value("${app.beneficiary-rule.build-delay-duration}") String beneficiaryRulesBuildDelay, BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, OnboardingContextHolderService onboardingContextHolderService, InitInitiativeCounterService initInitiativeCounterService) {
        this.beneficiaryRulesBuildDelay = Duration.parse(beneficiaryRulesBuildDelay);
        this.beneficiaryRule2DroolsRule = beneficiaryRule2DroolsRule;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.initInitiativeCounterService = initInitiativeCounterService;
    }

    @Override
    public void execute(Flux<Initiative2BuildDTO> initiativeBeneficiaryRuleDTOFlux) {
        initiativeBeneficiaryRuleDTOFlux
                .map(beneficiaryRule2DroolsRule) // TODO handle null value due to invalid ruleit.gov.pagopa.admissibility.service.build.BeneficiaryRuleBuilderMediatorService
                .flatMap(droolsRuleRepository::save)
                .map(i->{
                    onboardingContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    return i;
                })
                .flatMap(this::initializeCounters)
                .buffer(beneficiaryRulesBuildDelay)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(onboardingContextHolderService::setBeneficiaryRulesKieContainer);
    }

    private Mono<DroolsRule> initializeCounters(DroolsRule droolsRule) {
        return initInitiativeCounterService.initCounters(droolsRule.getInitiativeConfig())
                .then(Mono.just(droolsRule));
    }
}
