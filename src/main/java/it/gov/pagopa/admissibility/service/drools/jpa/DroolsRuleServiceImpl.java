package it.gov.pagopa.admissibility.service.drools.jpa;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DroolsRuleServiceImpl implements DroolsRuleService {
    private final DroolsRuleRepository droolsRuleRepository;

    public DroolsRuleServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<DroolsRule> save(DroolsRule droolsRule) {
        return droolsRule==null ? Mono.empty() : droolsRuleRepository.save(droolsRule);
    }

    @Override
    public Flux<DroolsRule> findAll() {
        return droolsRuleRepository.findAll();
    }
}
