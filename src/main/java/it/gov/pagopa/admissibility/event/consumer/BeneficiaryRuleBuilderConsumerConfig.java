package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.service.BeneficiaryRuleBuilderMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class BeneficiaryRuleBuilderConsumerConfig {

    @Autowired
    private BeneficiaryRuleBuilderMediatorService beneficiaryRuleBuilderMediatorService;

    @Bean
    public Consumer<Flux<Initiative2BuildDTO>> beneficiaryRuleBuilderConsumer() {
        return beneficiaryRuleBuilderMediatorService::execute;
    }
}