package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

public interface AdmissibilityEvaluatorMediatorService {

    Flux<EvaluationDTO> execute(Flux<Message<String>> messageFlux);
}
