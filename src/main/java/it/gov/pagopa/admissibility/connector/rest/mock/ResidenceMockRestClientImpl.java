package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
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
public class ResidenceMockRestClientImpl implements ResidenceMockRestClient{

    private static final String RESIDENCE_URI = "/residence/user/{userId}";
    private final WebClient webClient;

    public ResidenceMockRestClientImpl(@Value("${app.idpay-mock.base-url}") String idpayMockBaseUrl,
                                       WebClient.Builder webClientBuilder) {
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
                x -> "httpStatus %s".formatted(x.getStatusCodeValue())
        )
                .map(HttpEntity::getBody);
    }
}
