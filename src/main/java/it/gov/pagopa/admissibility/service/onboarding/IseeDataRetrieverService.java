package it.gov.pagopa.admissibility.service.onboarding;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

public interface IseeDataRetrieverService {

    Mono<Map<String, BigDecimal>> retrieveUserIsee(String userId);
}
