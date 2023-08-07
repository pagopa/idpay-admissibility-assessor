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
                .map(d -> {
                    log.info("[DELETE_DROOLS_RULE] Drools Rule deleted on initiative {}", initiativeId);
                    auditUtilities.logDeletedDroolsRule(initiativeId);
                    return d;
                });
    }

    private Mono<Void> deleteInitiativeCounters(String initiativeId) {
        return initiativeCountersRepository.deleteById(initiativeId)
                .map(i -> {
                    log.info("[DELETE_INITIATIVE_COUNTERS] Initiative counters deleted on initiative {}", initiativeId);
                    auditUtilities.logDeletedInitiativeCounters(initiativeId);
                    return i;
                });
    }

    private Mono<Void> deleteOnboardingFamilies(String initiativeId) {
        return onboardingFamiliesRepository.deleteByInitiativeId(initiativeId)
                .map(familyId -> {
                    log.info("[DELETE_FAMILIES] Families deleted on initiative {}", initiativeId);
                    auditUtilities.logDeletedOnboardingFamilies(familyId.getFamilyId(), initiativeId);
                    return familyId;
                })
                .then();
    }
}
