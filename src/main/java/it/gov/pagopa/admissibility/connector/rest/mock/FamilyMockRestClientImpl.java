package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class FamilyMockRestClientImpl implements FamilyMockRestClient {

    private static final String MOCK_FAMILY_URI = "/idpay/mock/family/user/{userId}";

    private final int idpayMockRetryDelay;
    private final long idpayMockMaxAttempts;
    private final WebClient webClient;
    private final String familyUri;

    public FamilyMockRestClientImpl(
            @Value("${app.inps-mock.enabled:false}") boolean mockEnabled,
            @Value("${app.inps-mock.base-url}") String inpsMockBaseUrl,
            @Value("${app.inps-real.base-url}") String inpsRealBaseUrl,
            @Value("${app.inps-mock.retry.delay-millis}") int inpsMockRetryDelay,
            @Value("${app.inps-mock.retry.max-attempts}") long inpsMockMaxAttempts,
            WebClient.Builder webClientBuilder) {

        this.idpayMockRetryDelay = inpsMockRetryDelay;
        this.idpayMockMaxAttempts = inpsMockMaxAttempts;

        String baseUrl = mockEnabled ? inpsMockBaseUrl : inpsRealBaseUrl;
        this.familyUri = mockEnabled ? MOCK_FAMILY_URI : "";

        log.info("[FAMILY_CLIENT] Using {} base URL: {}", mockEnabled ? "MOCK" : "REAL", baseUrl);

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Mono<Family> retrieveFamily(String userId) {
        return PerformanceLogger.logTimingOnNext(
                        "FAMILY_INTEGRATION",
                        webClient
                                .method(HttpMethod.GET)
                                .uri(familyUri.isEmpty() ? "" : familyUri, Map.of("userId", userId))
                                .retrieve()
                                .toEntity(Family.class),
                        x -> "httpStatus %s".formatted(x.getStatusCode().value())
                )
                .map(HttpEntity::getBody)
                .retryWhen(Retry.fixedDelay(idpayMockMaxAttempts, Duration.ofMillis(idpayMockRetryDelay))
                        .filter(ex -> {
                            boolean retry = ex instanceof WebClientResponseException.TooManyRequests;
                            if (retry) {
                                log.info("[FAMILY_INTEGRATION][FAMILY_RETRIEVE] Retrying due to exception: {}: {}",
                                        ex.getClass().getSimpleName(), ex.getMessage());
                            }
                            return retry;
                        })
                );
    }
}
