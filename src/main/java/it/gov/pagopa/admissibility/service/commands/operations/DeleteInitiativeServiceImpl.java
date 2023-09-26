package it.gov.pagopa.admissibility.service.commands.operations;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{

    private final DroolsRuleRepository droolsRuleRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final AuditUtilities auditUtilities;
    private final OnboardingContextHolderService onboardingContextHolderService;

    public DeleteInitiativeServiceImpl(DroolsRuleRepository droolsRuleRepository, InitiativeCountersRepository initiativeCountersRepository, OnboardingFamiliesRepository onboardingFamiliesRepository, AuditUtilities auditUtilities, OnboardingContextHolderService onboardingContextHolderService) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.auditUtilities = auditUtilities;
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public Mono<String> execute(String initiativeId, int pageSize, long delay) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return  deleteDroolsRule(initiativeId)
                .then(deleteInitiativeCounters(initiativeId))
                .then(deleteOnboardingFamilies(initiativeId, pageSize, delay))
                .then(Mono.just(initiativeId));
    }

    private Mono<Void> deleteDroolsRule(String initiativeId) {
        return droolsRuleRepository.deleteById(initiativeId)
                .doOnSuccess(d -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: beneficiary_rule", initiativeId);
                    auditUtilities.logDeletedDroolsRule(initiativeId);
                })
                .then(onboardingContextHolderService.refreshKieContainerCacheMiss())
                .then();
    }

    private Mono<Void> deleteInitiativeCounters(String initiativeId) {
        return initiativeCountersRepository.deleteById(initiativeId)
                .doOnSuccess(i -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: initiative_counters", initiativeId);
                    auditUtilities.logDeletedInitiativeCounters(initiativeId);
                });
    }

    private Mono<Void> deleteOnboardingFamilies(String initiativeId, int pageSize, long delay) {

        return onboardingFamiliesRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(of -> onboardingFamiliesRepository.deleteById(of.getId())
                        .then(Mono.just(of).delayElement(Duration.ofMillis(delay))), pageSize)
                .doOnNext(familyId -> auditUtilities.logDeletedOnboardingFamilies(familyId.getFamilyId(), initiativeId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: onboarding_families", initiativeId));
    }
}