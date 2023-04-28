package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class InitiativeStatusServiceImpl implements InitiativeStatusService {

    private final DroolsRuleRepository droolsRuleRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitiativeStatusServiceImpl(DroolsRuleRepository droolsRuleRepository, InitiativeCountersRepository initiativeCountersRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Mono<InitiativeStatusDTO> getInitiativeStatusAndBudgetAvailable(String initiativeId) {
        return droolsRuleRepository.findById(initiativeId)
                .flatMap(droolsRule ->
                        initiativeCountersRepository.findById(initiativeId)
                                .map(initiativeCounters -> {
                                    InitiativeConfig initiativeConfig = droolsRule.getInitiativeConfig();
                                InitiativeStatusDTO initiativeStatus = new InitiativeStatusDTO();
                                initiativeStatus.setStatus(initiativeConfig.getStatus());
                                initiativeStatus.setBudgetAvailable(
                                        isInitiativeBudgetAvailable(
                                                initiativeCounters.getResidualInitiativeBudgetCents(),
                                                initiativeConfig.getBeneficiaryInitiativeBudget()
                                        )
                                );
                                return initiativeStatus;
                                })
                );
    }

    private boolean isInitiativeBudgetAvailable(Long residualBudget, BigDecimal beneficiaryBudget) {
        BigDecimal residualBudgetBigDecimal = BigDecimal.valueOf(residualBudget);
        return residualBudgetBigDecimal.compareTo(beneficiaryBudget.multiply(BigDecimal.valueOf(100))) > -1;
    }
}
