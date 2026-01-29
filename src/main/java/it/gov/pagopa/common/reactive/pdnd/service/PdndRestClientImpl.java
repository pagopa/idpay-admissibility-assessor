package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.common.pdnd.generated.ApiClient;
import it.gov.pagopa.common.pdnd.generated.api.AuthApi;
import it.gov.pagopa.common.pdnd.generated.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

@Service
public class PdndRestClientImpl implements PdndRestClient {

    private static final String CLIENT_ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String GRANT_TYPE = "client_credentials";

    private final AuthApi authApi;

    public PdndRestClientImpl(
            @Value("${app.pdnd.base-url}") String pdndAccessTokenBaseUrl,
            WebClient.Builder webClientBuilder) {

        ConnectionProvider provider =
                ConnectionProvider.builder("pdnd-auth-pool")
                        .maxConnections(20)
                        .maxIdleTime(Duration.ofSeconds(20))
                        .maxLifeTime(Duration.ofMinutes(1))
                        .evictInBackground(Duration.ofSeconds(30))
                        .build();

        HttpClient httpClient =
                HttpClient.create(provider)
                        .responseTimeout(Duration.ofSeconds(10))
                        .keepAlive(true);

        WebClient webClient = webClientBuilder
                .clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        ApiClient newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(pdndAccessTokenBaseUrl);
        this.authApi = new AuthApi(newApiClient);
    }

    @Override
    public Mono<ClientCredentialsResponseDTO> createToken(String clientId, String clientAssertion) {
        return PerformanceLogger.logTimingOnNext(
                "PDND_AUTH",
                authApi.createToken(
                        clientAssertion,
                        CLIENT_ASSERTION_TYPE,
                        GRANT_TYPE,
                        clientId
                ),
                x -> clientId);
    }
}
