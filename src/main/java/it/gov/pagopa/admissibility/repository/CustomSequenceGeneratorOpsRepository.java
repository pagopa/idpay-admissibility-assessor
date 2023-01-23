package it.gov.pagopa.admissibility.repository;

import reactor.core.publisher.Mono;

public interface CustomSequenceGeneratorOpsRepository {
    Mono<Long> generateSequence(String sequenceId);
}