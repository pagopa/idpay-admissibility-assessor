package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class BeneficiaryRuleConsumerConfig {

    @Bean
    public Consumer<InitiativeBeneficiaryRuleDTO> beneficiaryRuleConsumer() {
        return rule -> log.info("Rule: {}", rule);
    }
}
