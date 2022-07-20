package it.gov.pagopa.admissibility.event.processor;


import it.gov.pagopa.admissibility.service.onboarding.AdmissibilityMediatorService;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class AdmissibilityProcessorConfig {

    private final AdmissibilityMediatorService admissibilityMediatorService;

    public AdmissibilityProcessorConfig(AdmissibilityMediatorService admissibilityMediatorService) {
        this.admissibilityMediatorService = admissibilityMediatorService;
    }

    /**
     *  Read from the topic ${KAFKA_TOPIC_ONBOARDING} and publish to topic ${KAFKA_TOPIC_ONBOARDING_OUTCOME}
     */
    @Bean
    public Function<Flux<OnboardingDTO>, Flux<EvaluationDTO>> admissibilityProcessor() {
        return this.admissibilityMediatorService::execute;
    }

    // TODO error handling
}
