package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.utils.Utils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class InitInitiativeCounterServiceImpl implements InitInitiativeCounterService {

    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitInitiativeCounterServiceImpl(InitiativeCountersRepository initiativeCountersRepository) {
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Mono<InitiativeCounters> initCounters(InitiativeConfig initiative) {
        return initiativeCountersRepository.findById(initiative.getInitiativeId())
                .map(counter2update -> {
                    final long initiativeBudgetCents = Utils.euro2Cents(initiative.getInitiativeBudget());
                    final long deltaBudget = initiativeBudgetCents - counter2update.getInitiativeBudgetCents();

                    counter2update.setInitiativeBudgetCents(initiativeBudgetCents);
                    counter2update.setResidualInitiativeBudgetCents(counter2update.getResidualInitiativeBudgetCents() + deltaBudget);
                    return counter2update;
                })
                .flatMap(initiativeCountersRepository::save)
                .switchIfEmpty(initiativeCountersRepository.save(
                        initiativeConfig2InitiativeCounter(initiative)));
    }

    private InitiativeCounters initiativeConfig2InitiativeCounter(InitiativeConfig initiative) {
        final long initiativeBudgetCents = Utils.euro2Cents(initiative.getInitiativeBudget());
        return InitiativeCounters.builder()
                .id(initiative.getInitiativeId())
                .initiativeBudgetCents(initiativeBudgetCents)
                .residualInitiativeBudgetCents(initiativeBudgetCents)
                .build();
    }
}
