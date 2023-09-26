package it.gov.pagopa.admissibility.connector.rest.pdnd;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
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
public class PdndRestClientImpl implements PdndRestClient {

    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String GRANT_TYPE = "client_credentials";

    private final AuthApi authApi;

    public PdndRestClientImpl(@Value("${app.pdnd.base-url}") String pdndAccessTokenBaseUrl,

                              @Value("${app.pdnd.web-client.timeouts.connect-timeout-millis}") int pdndConnectTimeOutMillis,
                              @Value("${app.pdnd.web-client.timeouts.response-timeout-millis}") int pdndResponseTimeOutMillis,
                              @Value("${app.pdnd.web-client.timeouts.read-timeout-handler}") int pdndReadTimeoutHandlerMillis) {
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
    public Mono<ClientCredentialsResponseDTO>
    createToken(String clientId, String clientAssertion) {
        return authApi.createToken(
                clientAssertion,
                CLIENT_ASSERTION_TYPE,
                GRANT_TYPE,
                clientId
        );
    }
}