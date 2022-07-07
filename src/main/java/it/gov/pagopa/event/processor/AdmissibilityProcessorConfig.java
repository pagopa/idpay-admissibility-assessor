package it.gov.pagopa.event.processor;


import it.gov.pagopa.dto.EvaluationDTO;
import it.gov.pagopa.dto.OnboardingDTO;
import it.gov.pagopa.service.AdmissibilityMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class AdmissibilityProcessorConfig {

    private final AdmissibilityMediatorService admService;

    public AdmissibilityProcessorConfig(AdmissibilityMediatorService admService) {
        this.admService = admService;
    }

    /**
     *  Read from the topic ${KAFKA_TOPIC_ONBOARDING} and publish to topic ${KAFKA_TOPIC_ONBOARDING_OUTCOME}
     */
    @Bean
    public Function<Flux<OnboardingDTO>, Flux<EvaluationDTO>> admissibilityProcessor() {
        return this.admService::execute;
    }
}
