package it.gov.pagopa.admissibility.mock.isee.controller;

import it.gov.pagopa.admissibility.mock.isee.service.IseeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class IseeControllerImpl implements IseeController {

    private final IseeService iseeService;

    public IseeControllerImpl(IseeService iseeService) {
        this.iseeService = iseeService;
    }

    @Override
    public Mono<Void> createIsee(String userId, IseeRequestDTO iseeRequestDTO) {
        log.info("[CREATE_ISEE] ISEE creation request for the user {}", userId);
        return iseeService.saveIsee(userId, iseeRequestDTO)
                .then();
    }

}
