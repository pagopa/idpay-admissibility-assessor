package it.gov.pagopa.admissibility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRule;
import it.gov.pagopa.admissibility.service.build.InitInitiativeCounterService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
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
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public BeneficiaryRuleBuilderMediatorServiceImpl(@Value("${app.beneficiary-rule.build-delay-duration}") String beneficiaryRulesBuildDelay, BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, OnboardingContextHolderService onboardingContextHolderService, InitInitiativeCounterService initInitiativeCounterService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.beneficiaryRulesBuildDelay = Duration.parse(beneficiaryRulesBuildDelay);
        this.beneficiaryRule2DroolsRule = beneficiaryRule2DroolsRule;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.initInitiativeCounterService = initInitiativeCounterService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(Initiative2BuildDTO.class);
    }

    @Override
    public void execute(Flux<Message<String>> initiativeBeneficiaryRuleDTOFlux) {
        initiativeBeneficiaryRuleDTOFlux
                .flatMap(this::execute)
                .buffer(beneficiaryRulesBuildDelay)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(onboardingContextHolderService::setBeneficiaryRulesKieContainer);
    }

    private Mono<DroolsRule> execute(Message<String> message){
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .map(beneficiaryRule2DroolsRule)
                .flatMap(droolsRuleRepository::save)
                .map(i -> {
                    onboardingContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    return i;
                })
                .flatMap(this::initializeCounters)

                .onErrorResume(e->{
                    errorNotifierService.notifyBeneficiaryRuleBuilder(message, "An error occurred handling initiative", true, e);
                    return Mono.empty();
                });
    }

    private Initiative2BuildDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyBeneficiaryRuleBuilder(message, "Unexpected JSON", true, e));
    }

    private Mono<DroolsRule> initializeCounters(DroolsRule droolsRule) {
        return initInitiativeCounterService.initCounters(droolsRule.getInitiativeConfig())
                .then(Mono.just(droolsRule));
    }
}
