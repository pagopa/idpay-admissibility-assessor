package it.gov.pagopa.admissibility.connector.event.consumer;

import it.gov.pagopa.admissibility.service.build.BeneficiaryRuleBuilderMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class BeneficiaryRuleBuilderConsumerConfig {

    @Bean
    public Consumer<Flux<Message<String>>> beneficiaryRuleBuilderConsumer(BeneficiaryRuleBuilderMediatorService beneficiaryRuleBuilderMediatorService) {
        return beneficiaryRuleBuilderMediatorService::execute;
    }
}
