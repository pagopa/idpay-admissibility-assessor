package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InitiativeStatusServiceImpl implements InitiativeStatusService {

    private final OnboardingContextHolderService contextHolderService;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitiativeStatusServiceImpl(OnboardingContextHolderService contextHolderService, InitiativeCountersRepository initiativeCountersRepository) {
        this.contextHolderService = contextHolderService;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Mono<InitiativeStatusDTO> getInitiativeStatusAndBudgetAvailable(String initiativeId) {
        log.debug("[ADMISSIBILITY][INITIATIVE_STATUS] Fetching initiative having id: {}", initiativeId);

        return contextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(initiativeConfig ->
                        initiativeCountersRepository.findById(initiativeId)
                                .map(initiativeCounters -> {
                                    InitiativeStatusDTO initiativeStatus = new InitiativeStatusDTO();
                                    initiativeStatus.setStatus(initiativeConfig.getStatus());
                                    initiativeStatus.setBudgetAvailable(isInitiativeBudgetAvailable(
                                            initiativeConfig.getInitiativeBudgetCents(),
                                            initiativeCounters.getSpentInitiativeBudgetCents())
                                    );


                                    log.info("[ADMISSIBILITY][INITIATIVE_STATUS] Found initiative {} having status: {} budgetAvailable: {}",
                                            sanitizeForLog(initiativeId), sanitizeForLog(initiativeStatus.getStatus()), initiativeStatus.isBudgetAvailable());
                                    return initiativeStatus;
                                }));
    }


    private boolean isInitiativeBudgetAvailable(Long initiativeBudgetCents, Long spentInitiativeBudgetCents) {
        if (initiativeBudgetCents == null || spentInitiativeBudgetCents == null) {
            log.warn("[ADMISSIBILITY][INITIATIVE_STATUS] Missing budget info: initiativeBudgetCents={} spentInitiativeBudgetCents={}", initiativeBudgetCents, spentInitiativeBudgetCents);
            return false;
        }

        long residualBudget = initiativeBudgetCents - spentInitiativeBudgetCents;

        boolean available = residualBudget >= 10000;

        log.debug("[ADMISSIBILITY][INITIATIVE_STATUS] Calculated residualBudget={} => available={}", residualBudget, available);
        return available;
    }

    private static String sanitizeForLog(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
