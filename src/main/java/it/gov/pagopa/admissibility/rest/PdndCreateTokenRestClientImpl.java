package it.gov.pagopa.admissibility.rest;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.ApiClient;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.api.AuthApi;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class PdndCreateTokenRestClientImpl implements PdndCreateTokenRestClient {
    private AuthApi authApi;

    private final String clientAssertionType;
    private final String grantType;

    public PdndCreateTokenRestClientImpl(@Value("${app.pdnd.access.token-base-url}") String pdndAccessTokenBaseUrl,
                                         @Value("${app.pdnd.properties.clientAssertionType}") String clientAssertionType,
                                         @Value("${app.pdnd.properties.grant-type}") String grantType,

                                         @Value("${app.pdnd.web-client.timeouts.connect-timeout-millis}") int pdndConnectTimeOutMillis,
                                         @Value("${app.pdnd.web-client.timeouts.response-timeout-millis}") int pdndResponseTimeOutMillis,
                                         @Value("${app.pdnd.web-client.timeouts.read-timeout-handler}") int pdndReadTimeoutHandlerMillis) {
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, pdndConnectTimeOutMillis)
                .responseTimeout(Duration.ofMillis(pdndResponseTimeOutMillis))
                .doOnConnected( connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(pdndReadTimeoutHandlerMillis, TimeUnit.MILLISECONDS)));

        WebClient webClient = ApiClient.buildWebClientBuilder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        ApiClient  newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(pdndAccessTokenBaseUrl);
        authApi = new AuthApi(newApiClient);
    }

    @Override
    public Mono<ClientCredentialsResponseDTO> createToken(ApiKeysPDND pdndTokens) {
        return authApi.createToken(
                pdndTokens.getApiKeyClientAssertion(),
                clientAssertionType,
                grantType,
                pdndTokens.getApiKeyClientId()
        );
    }
}