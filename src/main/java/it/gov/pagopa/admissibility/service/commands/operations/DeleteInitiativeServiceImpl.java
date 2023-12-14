package it.gov.pagopa.admissibility.service.commands.operations;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final int pageSize;
    private final long delay;

    public DeleteInitiativeServiceImpl(DroolsRuleRepository droolsRuleRepository,
                                       InitiativeCountersRepository initiativeCountersRepository,
                                       OnboardingFamiliesRepository onboardingFamiliesRepository,
                                       AuditUtilities auditUtilities,
                                       @Value("${app.delete.paginationSize}") int pageSize,
                                       @Value("${app.delete.delayTime}") long delay) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.auditUtilities = auditUtilities;
        this.pageSize = pageSize;
        this.delay = delay;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return  execAndLogTiming("DELETE_DROOLS_RULE", initiativeId, deleteDroolsRule(initiativeId))
                .then(execAndLogTiming("DELETE_INITIATIVE_COUNTERS", initiativeId, deleteInitiativeCounters(initiativeId)))
                .then(execAndLogTiming("DELETE_ONBOARDING_FAMILIES", initiativeId, deleteOnboardingFamilies(initiativeId)))
                .then(Mono.just(initiativeId));
    }

    private Mono<?> execAndLogTiming(String deleteFlowName, String initiativeId, Mono<?> deleteMono) {
        return PerformanceLogger.logTimingFinally(deleteFlowName, deleteMono, initiativeId);
    }

    private Mono<Void> deleteDroolsRule(String initiativeId) {
        return droolsRuleRepository.removeById(initiativeId)
                .doOnNext(d -> {
                    log.info("[DELETE_INITIATIVE] Deleted {} initiative {} from collection: beneficiary_rule", d.getDeletedCount(), initiativeId);
                    auditUtilities.logDeletedDroolsRule(initiativeId);
                })
                .then();
    }

    private Mono<Void> deleteInitiativeCounters(String initiativeId) {
        return initiativeCountersRepository.removeById(initiativeId)
                .doOnNext(i -> {
                    log.info("[DELETE_INITIATIVE] Deleted {} initiative {} from collection: initiative_counters", i.getDeletedCount(), initiativeId);
                    auditUtilities.logDeletedInitiativeCounters(initiativeId);
                })
                .then();
    }

    private Mono<Void> deleteOnboardingFamilies(String initiativeId) {

        return onboardingFamiliesRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(of -> onboardingFamiliesRepository.deleteById(of.getId())
                        .then(Mono.just(of).delayElement(Duration.ofMillis(delay))), pageSize)
                .doOnNext(familyId -> auditUtilities.logDeletedOnboardingFamilies(familyId.getFamilyId(), initiativeId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: onboarding_families", initiativeId));
    }
}