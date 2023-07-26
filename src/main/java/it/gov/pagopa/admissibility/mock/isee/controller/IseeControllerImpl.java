package it.gov.pagopa.admissibility.mock.isee.controller;

import it.gov.pagopa.admissibility.mock.isee.service.IseeService;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class IseeControllerImpl implements IseeController {

    private final IseeService iseeService;

    public IseeControllerImpl(IseeService iseeService) {
        this.iseeService = iseeService;
    }

    @Override
    public Mono<Void> createIsee(String userId, IseeRequestDTO iseeRequestDTO) {
        return iseeService.saveIsee(userId, iseeRequestDTO)
                .then();
    }

}
