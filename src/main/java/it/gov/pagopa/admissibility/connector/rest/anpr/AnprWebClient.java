package it.gov.pagopa.admissibility.connector.rest.anpr;

import it.gov.pagopa.admissibility.config.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class AnprWebClient {
    private final WebClient.Builder webClientBuilder;


    public AnprWebClient(@Value("${app.anpr.web-client.ssl.cert}") String stringCert,
                         @Value("${app.anpr.web-client.ssl.key}") String clientKeyPem,

                         @Value("${app.anpr.web-client.timeouts.connect-timeout-millis}") int residenceAssessmentConnectTimeOutMillis,
                         @Value("${app.anpr.web-client.timeouts.response-timeout-millis}")  int residenceAssessmentResponseTimeoutMillis,
                         @Value("${app.anpr.web-client.timeouts.read-timeout-handler}") int residenceAssessmentReadTimeoutHandlerMillis) {


        HttpClient httpClientSecure = WebClientConfig
                .getHttpClientWithReadTimeoutHandlerConfig(residenceAssessmentConnectTimeOutMillis, residenceAssessmentResponseTimeoutMillis, residenceAssessmentReadTimeoutHandlerMillis)
                .secure(t -> t.sslContext(WebClientConfig.buildSSLHttpClient(stringCert, clientKeyPem)));

        webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClientSecure));
    }

    public WebClient.Builder getAnprWebClient() {
        return this.webClientBuilder;
    }
}