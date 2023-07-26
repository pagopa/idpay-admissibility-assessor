package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RequestMapping("idpay/isee/mock")
public interface MockIseeController {
    @PostMapping("/{userId}")
    Mono<Void> createIsee(@RequestParam("userId") String userId, @RequestBody IseeRequestDTO iseeRequestDTO);

    @Data
    @Builder
    class IseeRequestDTO {
        private Map<IseeTypologyEnum, BigDecimal> iseeTypeMap;
    }
}
