package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
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
public class ResidenceMockRestClientImpl implements ResidenceMockRestClient{

    private static final String RESIDENCE_URI = "/idpay/mock/residence/user/{userId}";
    private final int idpayMockRetryDelay;
    private final long idpayMockMaxAttempts;
    private final WebClient webClient;

    public ResidenceMockRestClientImpl(@Value("${app.idpay-mock.base-url}") String idpayMockBaseUrl,
                                       @Value("${app.idpay-mock.retry.delay-millis}") int idpayMockRetryDelay,
                                       @Value("${app.idpay-mock.retry.max-attempts}") long idpayMockMaxAttempts,
                                       WebClient.Builder webClientBuilder) {
        this.idpayMockRetryDelay = idpayMockRetryDelay;
        this.idpayMockMaxAttempts = idpayMockMaxAttempts;
        this.webClient = webClientBuilder.clone()
                .baseUrl(idpayMockBaseUrl)
                .build();
    }

    @Override
    public Mono<Residence> retrieveResidence(String userId) {
        return PerformanceLogger.logTimingOnNext("RESIDENCE_MOCK_INTEGRATION",
                webClient
                        .method(HttpMethod.GET)
                        .uri(RESIDENCE_URI, Map.of("userId", userId))
                        .retrieve()
                        .toEntity(Residence.class),
                x -> "httpStatus %s".formatted(x.getStatusCode().value())
        )
                .map(HttpEntity::getBody)

                .retryWhen(Retry.fixedDelay(idpayMockMaxAttempts, Duration.ofMillis(idpayMockRetryDelay))
                        .filter(ex -> {
                            boolean retry = (ex instanceof WebClientResponseException.TooManyRequests) || ex.getMessage().startsWith("Connection refused");
                            if (retry) {
                                log.info("[IDPAY_MOCK_INTEGRATION][RESIDENCE_RETRIEVE] Retrying invocation due to exception: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
                            }
                            return retry;
                        })
                );
    }
}
