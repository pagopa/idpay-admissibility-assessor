package it.gov.pagopa.common.reactive.rest.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    private final int connectTimeoutMillis;
    private final int responseTimeoutMillis;
    private final int readTimeoutHandlerMillis;
    private final int writeTimeoutHandlerMillis;

    public WebClientConfig(
            @Value("${app.web-client.connect.timeout.millis}") int connectTimeoutMillis,
            @Value("${app.web-client.response.timeout}") int responseTimeoutMillis,
            @Value("${app.web-client.read.handler.timeout}") int readTimeoutHandlerMillis,
            @Value("${app.web-client.write.handler.timeout}") int writeTimeoutHandlerMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.responseTimeoutMillis = responseTimeoutMillis;
        this.readTimeoutHandlerMillis = readTimeoutHandlerMillis;
        this.writeTimeoutHandlerMillis = writeTimeoutHandlerMillis;
    }

    public static HttpClient getHttpClientWithReadTimeoutHandlerConfig(int connectTimeoutMillis, int responseTimeoutMillis, int readTimeoutHandlerMillis){
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofMillis(responseTimeoutMillis))
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(readTimeoutHandlerMillis, TimeUnit.MILLISECONDS)));
    }
    public static HttpClient httpClientConfig(int connectTimeoutMillis, int responseTimeoutMillis, int readTimeoutHandlerMillis, int writeTimeoutHandlerMillis){
        return getHttpClientWithReadTimeoutHandlerConfig(connectTimeoutMillis,responseTimeoutMillis,readTimeoutHandlerMillis)
                .doOnConnected( connection -> connection.addHandlerLast(new WriteTimeoutHandler(writeTimeoutHandlerMillis, TimeUnit.MILLISECONDS)));
    }

    @Bean
    public WebClient.Builder webClientConfigure(){
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClientConfig(connectTimeoutMillis, responseTimeoutMillis,readTimeoutHandlerMillis, writeTimeoutHandlerMillis)));
    }

    public static SslContext buildSSLHttpClient(String stringCert, String clientKeyPem) {
        try(
                InputStream certInputStream = getCertInputStream(stringCert);
                InputStream keyInputStream = getKeyInputStream(clientKeyPem)
        ) {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .keyManager(certInputStream, keyInputStream);
            return getSslContext(sslContextBuilder);
        } catch (IOException e) {
            throw new IllegalStateException("Something went wrong creating ssl context",e);
        }
    }

    private static SslContext getSslContext(SslContextBuilder sslContextBuilder) throws SSLException {
        return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

    private static InputStream getCertInputStream(String stringCert) {
        return new ByteArrayInputStream(stringCert.getBytes(StandardCharsets.UTF_8));
    }

    private static InputStream getKeyInputStream(String clientKeyPem) {
        return new ByteArrayInputStream(clientKeyPem.getBytes(StandardCharsets.UTF_8));
    }
}