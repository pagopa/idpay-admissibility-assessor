package it.gov.pagopa.admissibility.mock.isee.controller;

import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RequestMapping("idpay/isee/mock")
public interface IseeController {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{userId}")
    Mono<Void> createIsee(@PathVariable String userId, @RequestBody IseeRequestDTO iseeRequestDTO);

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class IseeRequestDTO {
        private Map<IseeTypologyEnum, BigDecimal> iseeTypeMap;
    }
}
