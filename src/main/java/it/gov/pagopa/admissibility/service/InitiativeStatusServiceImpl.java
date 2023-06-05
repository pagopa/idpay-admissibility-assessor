package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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
        log.info("[ADMISSIBILITY][INITIATIVE_STATUS] Fetching initiative having id: {}", initiativeId);
        return contextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(initiativeConfig ->
                        initiativeCountersRepository.findById(initiativeId)
                                .map(initiativeCounters -> {
                                InitiativeStatusDTO initiativeStatus = new InitiativeStatusDTO();
                                initiativeStatus.setStatus(initiativeConfig.getStatus());
                                initiativeStatus.setBudgetAvailable(
                                        isInitiativeBudgetAvailable(
                                                initiativeCounters.getResidualInitiativeBudgetCents(),
                                                initiativeConfig.getBeneficiaryInitiativeBudget()
                                        )
                                );

                                log.info("[ADMISSIBILITY][INITIATIVE_STATUS] Found initiative {} having status: {} budgetAvailable: {}",
                                        initiativeId, initiativeStatus.getStatus(), initiativeStatus.isBudgetAvailable());
                                return initiativeStatus;
                                })
                );
    }

    private boolean isInitiativeBudgetAvailable(Long residualBudget, BigDecimal beneficiaryBudget) {
        BigDecimal residualBudgetBigDecimal = BigDecimal.valueOf(residualBudget);
        return residualBudgetBigDecimal.compareTo(beneficiaryBudget.multiply(BigDecimal.valueOf(100))) > -1;
    }
}
