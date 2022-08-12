package it.gov.pagopa.admissibility.event.processor;


import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.service.AdmissibilityEvaluatorMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class AdmissibilityProcessorConfig {

    private final AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService;

    public AdmissibilityProcessorConfig(AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService) {
        this.admissibilityEvaluatorMediatorService = admissibilityEvaluatorMediatorService;
    }

    /**
     *  Read from the topic ${KAFKA_TOPIC_ONBOARDING} and publish to topic ${KAFKA_TOPIC_ONBOARDING_OUTCOME}
     */
    @Bean
    public Function<Flux<Message<String>>, Flux<EvaluationDTO>> admissibilityProcessor() {
        return this.admissibilityEvaluatorMediatorService::execute;
    }

}
