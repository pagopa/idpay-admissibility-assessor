package it.gov.pagopa.admissibility.connector.rest.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesReqDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingStatusDTO;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

import static it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus.ONBOARDING_OK;

@Slf4j
@Service
public class OnboardingRestClientImpl implements OnboardingRestClient {
    private static final String GET_STATUS_URI = "/idpay/onboarding/{initiativeId}/{userId}/status";
    private static final String CHECK_PAST_ONBOARDINGS_SERIES_URI = "/idpay/onboarding/{initiativeId}/check-past-onboardings-series";
    private final int idpayOnboardingRetryDelay;
    private final long idpayOnboardingMaxAttempts;
    private final WebClient webClient;

    public OnboardingRestClientImpl(
            @Value("${app.onboarding-workflow.base-url}") String idpayOnboardingBaseUrl,
            @Value("${app.onboarding-workflow.retry.delay-millis}") int idpayOnboardingRetryDelay,
            @Value("${app.onboarding-workflow.retry.max-attempts}") long idpayOnboardingMaxAttempts,
            WebClient.Builder webClientBuilder) {
        this.idpayOnboardingRetryDelay = idpayOnboardingRetryDelay;
        this.idpayOnboardingMaxAttempts = idpayOnboardingMaxAttempts;
        this.webClient = webClientBuilder.clone()
                .baseUrl(idpayOnboardingBaseUrl)
                .build();
    }

    @Override
    public Mono<Pair<Boolean, String>> alreadyOnboardingStatus(String initiativeId, String userId) {
        return PerformanceLogger.logTimingOnNext("ONBOARDING_STATUS_INTEGRATION",
                        webClient
                                .method(HttpMethod.GET)
                                .uri(GET_STATUS_URI, Map.of("initiativeId", initiativeId, "userId", userId))
                                .retrieve()
                                .onStatus(
                                        status -> status == HttpStatus.BAD_REQUEST || status == HttpStatus.NOT_FOUND,
                                        response -> Mono.empty()
                                )
                                .toEntity(OnboardingStatusDTO.class),
                        x -> "httpStatus %s".formatted(x.getStatusCode().value()))
                .map(response -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status == HttpStatus.OK && response.getBody() != null) {
                        OnboardingStatusDTO dto = response.getBody();
                        if (ONBOARDING_OK.toString().equals(dto.getStatus())) {
                            log.debug("Onboarding status ricevuto: {}", dto);
                            return Pair.of(true, userId);
                        }
                    }
                    return Pair.of(false, (String) null);
                })
                .defaultIfEmpty(Pair.of(false, null))
                .retryWhen(Retry.fixedDelay(idpayOnboardingMaxAttempts, Duration.ofMillis(idpayOnboardingRetryDelay))
                        .filter(ex -> {
                            boolean retry = ex instanceof WebClientResponseException.TooManyRequests;
                            if (retry) {
                                log.info("[IDPAY_ONBOARDING_STATUS_INTEGRATION][ALREADY_ONBOARDING] Retrying invocation due to exception: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
                            }
                            return retry;
                        })
                );
    }

    @Override
    public Mono<CheckPastOnboardingsSeriesResDTO> checkPastOnboardingsSeries(
            String initiativeId,
            CheckPastOnboardingsSeriesReqDTO request) {
        log.info("Sending check past onboardings series request for initiativeId {}", initiativeId);

        return PerformanceLogger.logTimingOnNext(
                "CHECK_PAST_ONBOARDINGS_SERIES",
                webClient
                        .post()
                        .uri(CHECK_PAST_ONBOARDINGS_SERIES_URI, Map.of("initiativeId", initiativeId))
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response ->
                                response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(body -> Mono.error(new IllegalStateException(
                                                "Check past onboardings series failed: httpStatus=%s, body=%s"
                                                        .formatted(response.statusCode().value(), body)
                                        )))
                        )
                        .toEntity(CheckPastOnboardingsSeriesResDTO.class),
                response -> "httpStatus %s".formatted(response.getStatusCode().value())
        )
                .flatMap(response -> {
                    CheckPastOnboardingsSeriesResDTO body = response.getBody();
                    if (body == null || body.getPastOnboardings() == null) {
                        return Mono.error(new IllegalStateException(
                                "Invalid response from checkPastOnboardingsSeries API: missing pastOnboardings"));
                    }
                    log.debug("Check past onboardings series response received: {}", body);
                    return Mono.just(body);
                })
                .retryWhen(Retry.fixedDelay(idpayOnboardingMaxAttempts, Duration.ofMillis(idpayOnboardingRetryDelay))
                        .filter(ex -> {
                            boolean retry = ex instanceof WebClientResponseException.TooManyRequests;
                            if (retry) {
                                log.info("[CHECK_PAST_ONBOARDINGS_SERIES] Retrying invocation due to exception: {}: {}",
                                        ex.getClass().getSimpleName(), ex.getMessage());
                            }
                            return retry;
                        })
                );
    }
}
