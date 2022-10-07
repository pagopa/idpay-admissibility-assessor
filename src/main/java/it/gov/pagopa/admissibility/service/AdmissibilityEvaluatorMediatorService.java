package it.gov.pagopa.admissibility.service;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

public interface AdmissibilityEvaluatorMediatorService {

    void execute(Flux<Message<String>> messageFlux);
}
