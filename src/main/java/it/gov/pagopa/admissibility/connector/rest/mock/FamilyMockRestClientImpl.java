package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class FamilyMockRestClientImpl implements FamilyMockRestClient{
    private static final String FAMILY_URI = "/idpay/mock/family/user/{userId}";
    private final WebClient webClient;

    public FamilyMockRestClientImpl(@Value("${app.idpay-mock.base-url}") String idpayMockBaseUrl,
                                    WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(idpayMockBaseUrl)
                .build();
    }

    @Override
    public Mono<Family> retrieveFamily(String userId) {
        return PerformanceLogger.logTimingOnNext("FAMILY_MOCK_INTEGRATION",
                        webClient
                                .method(HttpMethod.GET)
                                .uri(FAMILY_URI, Map.of("userId", userId))
                                .retrieve()
                                .toEntity(Family.class),
                        x -> "httpStatus %s".formatted(x.getStatusCode().value())
                )
                .map(HttpEntity::getBody);
    }
}
