package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.service.build.BeneficiaryRuleBuilderMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class BeneficiaryRuleBuilderConsumerConfig {

    @Autowired
    private BeneficiaryRuleBuilderMediatorService beneficiaryRuleBuilderMediatorService;

    @Bean
    public Consumer<Flux<Message<String>>> beneficiaryRuleBuilderConsumer() {
        return beneficiaryRuleBuilderMediatorService::execute;
    }
}
