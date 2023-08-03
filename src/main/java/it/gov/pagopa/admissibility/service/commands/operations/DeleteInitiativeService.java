package it.gov.pagopa.admissibility.service.commands.operations;

import reactor.core.publisher.Mono;

public interface DeleteInitiativeService {
    Mono<String> execute(String initiativeId);
}
