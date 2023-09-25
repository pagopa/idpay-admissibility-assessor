package it.gov.pagopa.admissibility.controller.ru_controller;

import it.gov.pagopa.admissibility.service.ru.RuService;
import reactor.core.publisher.Mono;

public class RuControllerImpl implements RuController{
    private final RuService ruService;

    public RuControllerImpl(RuService ruService) {
        this.ruService = ruService;
    }

    @Override
    public Mono<Void> createRecord(String initiativeId, int numberOfRecord) {
        return ruService.createRecord(initiativeId, numberOfRecord);
    }

    @Override
    public Mono<Void> deleteOnboardingFamiliesExpand(String initiativeId, int pageSize, long delay) {
        return ruService.deleteOnboardingFamiliesExpand(initiativeId, pageSize, delay);
    }

    @Override
    public Mono<Void> deleteOnboardingFamiliesRangeLimit(String initiativeId, int pageSize, long delay) {
        return ruService.deleteOnboardingFamiliesRangeLimit(initiativeId, pageSize, delay);
    }
}
