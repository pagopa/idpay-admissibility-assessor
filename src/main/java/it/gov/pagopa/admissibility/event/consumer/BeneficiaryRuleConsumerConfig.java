package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.service.build.BeneficiaryRuleMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class BeneficiaryRuleConsumerConfig {

    @Autowired
    private BeneficiaryRuleMediatorService beneficiaryRuleMediatorService;

    @Bean
    public Consumer<Flux<Initiative2BuildDTO>> beneficiaryRuleConsumer() {
        return beneficiaryRuleMediatorService::execute;
    }
}
