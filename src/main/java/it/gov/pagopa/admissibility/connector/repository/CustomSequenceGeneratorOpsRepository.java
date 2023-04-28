package it.gov.pagopa.admissibility.connector.repository;

import reactor.core.publisher.Mono;

public interface CustomSequenceGeneratorOpsRepository {
    Mono<Long> nextValue(String sequenceId);
}