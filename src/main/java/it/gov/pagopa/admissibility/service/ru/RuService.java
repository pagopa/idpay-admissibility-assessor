package it.gov.pagopa.admissibility.service.ru;

import reactor.core.publisher.Mono;

public interface RuService {
    Mono<Void> createRecord(String initiativeId, int numberOfRecord);
    Mono<Void> deleteOnboardingFamiliesExpand(String initiativeId, int pageSize, long delay);
    Mono<Void> deleteOnboardingFamiliesRangeLimit(String initiativeId, int pageSize, long delay);
}
