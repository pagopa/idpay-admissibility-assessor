package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

public class InitiativeStatusServiceImpl implements InitiativeStatusService {

    private final DroolsRuleRepository droolsRuleRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitiativeStatusServiceImpl(DroolsRuleRepository droolsRuleRepository, InitiativeCountersRepository initiativeCountersRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Flux<InitiativeStatusDTO> getInitiativeStatusAndBudgetAvailable(String initiativeId) {
        InitiativeStatusDTO initiativeStatus = new InitiativeStatusDTO();
        droolsRuleRepository.findById(initiativeId)
                .flatMap(droolsRule -> {
                    InitiativeConfig initiativeConfig = droolsRule.getInitiativeConfig();
                    initiativeCountersRepository.findById(initiativeId)
                            .flatMap(initiativeCounters -> {
                                initiativeStatus.setBudgetAvailable(
                                        isInitiativeBudgetAvailable(initiativeCounters.getResidualInitiativeBudgetCents(),
                                                initiativeConfig.getBeneficiaryInitiativeBudget())
                                );
                                return null;
                            });
                    return null;
                });
        // TODO initiativeStatus.setStatus()
        return Flux.just(initiativeStatus);
    }

    private boolean isInitiativeBudgetAvailable(Long residualBudget, BigDecimal beneficiaryBudget) {
        BigDecimal residualBudgetBigDecimal = BigDecimal.valueOf(residualBudget/100);
        return residualBudgetBigDecimal.compareTo(beneficiaryBudget) > -1;
    }
}
