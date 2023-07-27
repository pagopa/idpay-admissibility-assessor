package it.gov.pagopa.admissibility.mock.isee.service;

import it.gov.pagopa.admissibility.mock.isee.controller.IseeController;
import it.gov.pagopa.admissibility.mock.isee.model.Isee;
import reactor.core.publisher.Mono;

public interface IseeService {
    Mono<Isee> saveIsee(String userId, IseeController.IseeRequestDTO iseeRequestDTO);
}
