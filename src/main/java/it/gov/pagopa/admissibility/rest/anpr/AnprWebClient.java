package it.gov.pagopa.admissibility.rest.anpr;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.gov.pagopa.admissibility.config.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class AnprWebClient {
    private final String stringCert;
    private final String clientKeyPem;

    private final HttpClient httpClientSecure;

    public AnprWebClient(@Value("${app.anpr.web-client.secure.cert}") String stringCert,
                         @Value("${app.anpr.web-client.secure.key}") String clientKeyPem,

                         @Value("${app.anpr.web-client.timeouts.connect-timeout-millis}") int residenceAssessmentConnectTimeOutMillis,
                         @Value("${app.anpr.web-client.timeouts.response-timeout-millis}")  int residenceAssessmentResponseTimeoutMillis,
                         @Value("${app.anpr.web-client.timeouts.read-timeout-handler}") int residenceAssessmentReadTimeoutHandlerMillis) {
        this.stringCert = stringCert;
        this.clientKeyPem = clientKeyPem;

        httpClientSecure = WebClientConfig
                .getHttpClientWithReadTimeoutHandlerConfig(residenceAssessmentConnectTimeOutMillis, residenceAssessmentResponseTimeoutMillis, residenceAssessmentReadTimeoutHandlerMillis)
                .secure(t -> t.sslContext(buildSSLHttpClient()));
    }

    public HttpClient getHttpClientSecure() {
        return this.httpClientSecure;
    }

    private SslContext buildSSLHttpClient() {
        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .keyManager(getCertInputStream(stringCert), getKeyInputStream(clientKeyPem));
            return getSslContext(sslContextBuilder);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private SslContext getSslContext(SslContextBuilder sslContextBuilder) throws SSLException {
        return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

    private InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(stringCert.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(clientKeyPem.getBytes(StandardCharsets.UTF_8));
    }
}