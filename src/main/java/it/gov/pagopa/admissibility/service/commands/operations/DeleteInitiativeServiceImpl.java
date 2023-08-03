package it.gov.pagopa.admissibility.service.commands.operations;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{

    private final DroolsRuleRepository droolsRuleRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final AuditUtilities auditUtilities;

    public DeleteInitiativeServiceImpl(DroolsRuleRepository droolsRuleRepository, InitiativeCountersRepository initiativeCountersRepository, OnboardingFamiliesRepository onboardingFamiliesRepository, AuditUtilities auditUtilities) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return  deleteDroolsRule(initiativeId)
                .then(deleteInitiativeCounters(initiativeId))
                .then(deleteOnboardingFamilies(initiativeId))
                .then(Mono.just(initiativeId));
    }

    private Mono<Void> deleteDroolsRule(String initiativeId) {
        return droolsRuleRepository.deleteById(initiativeId)
                .doOnNext(d -> {
                    log.info("[DELETE_DROOLS_RULE] Drools Rule deleted on initiative {}", initiativeId);
                auditUtilities.logDeletedDroolsRule(initiativeId);
                });
    }

    private Mono<Void> deleteInitiativeCounters(String initiativeId) {
        return initiativeCountersRepository.deleteById(initiativeId)
                .doOnNext(i -> {
                    log.info("[DELETE_INITIATIVE_COUNTERS] Initiative counters deleted on initiative {}", initiativeId);
                    auditUtilities.logDeletedInitiativeCounters(initiativeId);
                });
    }

    private Mono<Void> deleteOnboardingFamilies(String initiativeId) {
        return onboardingFamiliesRepository.deleteByInitiativeId(initiativeId)
                .doOnNext(familyId -> {
                    log.info("[DELETE_FAMILIES] Families deleted on initiatve {}", initiativeId);
                    auditUtilities.logDeletedOnboardingFamilies(familyId.getFamilyId(), initiativeId);
                })
                .then();
    }
}
