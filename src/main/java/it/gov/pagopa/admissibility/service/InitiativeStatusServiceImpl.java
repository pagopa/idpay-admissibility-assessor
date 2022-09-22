package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.InitiativeStatusDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
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
        InitiativeStatusDTO initiativeStatus = new InitiativeStatusDTO();
        return droolsRuleRepository.findById(initiativeId)
                .flatMap(droolsRule -> {
                    InitiativeConfig initiativeConfig = droolsRule.getInitiativeConfig();
                    initiativeCountersRepository.findById(initiativeId)
                            .flatMap(initiativeCounters -> {
                                initiativeStatus.setBudgetAvailable(
                                        isInitiativeBudgetAvailable(
                                                initiativeCounters.getResidualInitiativeBudgetCents(),
                                                initiativeConfig.getBeneficiaryInitiativeBudget()
                                        )
                                );
                                return Mono.just(initiativeCounters);
                            });
                    return Mono.just(droolsRule);
                })
                .then(Mono.just(initiativeStatus));

        // TODO initiativeStatus.setStatus()
    }

    private boolean isInitiativeBudgetAvailable(Long residualBudget, BigDecimal beneficiaryBudget) {
        BigDecimal residualBudgetBigDecimal = BigDecimal.valueOf(residualBudget/100);
        return residualBudgetBigDecimal.compareTo(beneficiaryBudget) > -1;
    }
}
