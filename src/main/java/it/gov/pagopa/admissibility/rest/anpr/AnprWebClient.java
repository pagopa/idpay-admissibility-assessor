package it.gov.pagopa.admissibility.rest.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.gov.pagopa.admissibility.config.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    public static SslContext getSslContext(SslContextBuilder sslContextBuilder) throws SSLException {
        return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

    public static InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(stringCert.getBytes(StandardCharsets.UTF_8));
    }

    public static InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(clientKeyPem.getBytes(StandardCharsets.UTF_8));
    }
}