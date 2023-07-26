package it.gov.pagopa.admissibility.mock.isee.controller;

import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RequestMapping("idpay/isee/mock")
public interface IseeController {
    @PostMapping("/{userId}")
    Mono<Void> createIsee(@PathVariable String userId, @RequestBody IseeRequestDTO iseeRequestDTO);

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class IseeRequestDTO {
        private Map<IseeTypologyEnum, BigDecimal> iseeTypeMap;
    }
}
