package it.gov.pagopa.event.consumer;

import it.gov.pagopa.dto.OnboardingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class OnboardingConsumerConfig {

    @Bean
    public Consumer<OnboardingDTO> onboardingConsumer() {
        return onb -> log.info("Onboarding info: {}", onb);
    }
}
