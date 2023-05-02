package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import reactor.core.publisher.Mono;

public interface PdndAccessTokenRetrieverService {
    Mono<String> getToken(ApiKeysPDND pdndToken);
}