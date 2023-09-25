package it.gov.pagopa.admissibility.controller.ru_controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Component that exposes APIs
 * */
@RequestMapping("/idpay/admissibility")
public interface RuController {

    @PostMapping(value = "/initiative/{initiativeId}")
    Mono<Void> createRecord(@PathVariable("initiativeId") String initiativeId, @RequestParam("numberOfRecord") int numberOfRecord);
    @DeleteMapping(value = "/expand/initiative/{initiativeId}")
    Mono<Void> deleteOnboardingFamiliesExpand (@PathVariable("initiativeId") String initiativeId,
                                               @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                               @RequestParam(name = "delay", defaultValue = "5000") long delay);
    @DeleteMapping(value = "/rangeLimit/initiative/{initiativeId}")
    Mono<Void> deleteOnboardingFamiliesRangeLimit (@PathVariable("initiativeId") String initiativeId,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                                   @RequestParam(name = "delay", defaultValue = "5000") long delay);
}
