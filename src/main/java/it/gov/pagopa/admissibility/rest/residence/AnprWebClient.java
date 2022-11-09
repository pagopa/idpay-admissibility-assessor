package it.gov.pagopa.admissibility.rest.residence;

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
import java.util.Base64;

@Component
public class AnprWebClient {
    private final String stringCert;
    private final String clientKeyPem;

    private final HttpClient httpClientSecure;

    public AnprWebClient(@Value("${app.anpr.secure.cert}") String stringCert,
                         @Value("${app.anpr.secure.key}") String clientKeyPem,

                         @Value("${app.anpr.c020-residenceAssessment.base-url}") String residenceAssessmentBaseUrl,
                         @Value("${app.anpr.web-client.timeouts.connect-timeout-millis}") int residenceAssessmentConnectTimeOutMillis,
                         @Value("${app.anpr.web-client.timeouts.response-timeout-millis}")  int residenceAssessmentResponseTimeoutMillis,
                         @Value("${app.anpr.web-client.timeouts.read-timeout-handler}") int residenceAssessmentReadTimeoutHandlerMillis) {
        this.stringCert = stringCert;
        this.clientKeyPem = clientKeyPem;

        httpClientSecure = WebClientConfig
                .getHttpClientWithReadTimeoutHandlerConfig(residenceAssessmentConnectTimeOutMillis, residenceAssessmentResponseTimeoutMillis, residenceAssessmentReadTimeoutHandlerMillis)
                .secure(t -> t.sslContext(buildSSLHttpClient()))
                .baseUrl(residenceAssessmentBaseUrl);
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
        return new ByteArrayInputStream(Base64.getDecoder().decode(stringCert));
    }

    private InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
    }
}