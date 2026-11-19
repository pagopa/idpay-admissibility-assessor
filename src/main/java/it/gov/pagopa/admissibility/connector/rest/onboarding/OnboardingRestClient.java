package it.gov.pagopa.admissibility.connector.rest.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesReqDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResDTO;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

public interface OnboardingRestClient {
    Mono<Pair<Boolean, String>> alreadyOnboardingStatus(String initiativeId, String userId);

    Mono<CheckPastOnboardingsSeriesResDTO> checkPastOnboardingsSeries(String initiativeId, CheckPastOnboardingsSeriesReqDTO request);
}
