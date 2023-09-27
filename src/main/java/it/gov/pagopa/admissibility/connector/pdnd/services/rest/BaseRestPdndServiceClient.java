package it.gov.pagopa.admissibility.connector.pdnd.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.connector.pdnd.PdndRestClient;
import it.gov.pagopa.admissibility.connector.pdnd.components.JwtSignAlgorithmRetrieverService;
import it.gov.pagopa.admissibility.connector.pdnd.config.BasePdndServiceProviderConfig;
import it.gov.pagopa.admissibility.connector.pdnd.config.PdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.dto.PdndAuthData;
import it.gov.pagopa.admissibility.connector.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.admissibility.connector.pdnd.exception.PdndServiceTooManyRequestException;
import it.gov.pagopa.admissibility.connector.pdnd.services.BasePdndService;
import it.gov.pagopa.admissibility.connector.pdnd.utils.AgidUtils;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.Utils;
import it.gov.pagopa.common.web.utils.SslUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
public abstract class BaseRestPdndServiceClient<T, R> extends BasePdndService<R> {

    private final WebClient webClient;

    protected BaseRestPdndServiceClient(
            PdndServiceConfig<R> pdndServiceConfig,
            ObjectMapper objectMapper,
            PdndConfig pdndConfig,
            JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient,
            WebClient.Builder webClientBuilder,
            HttpClient httpClient) {
        super(pdndServiceConfig, objectMapper, pdndConfig, jwtSignAlgorithmRetrieverService, pdndRestClient);

        configureHttps(httpClient, pdndServiceConfig.getHttpsConfig());

        this.webClient = webClientBuilder.clone()
                .baseUrl(pdndServiceConfig.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static void configureHttps(HttpClient httpClient, BasePdndServiceProviderConfig.HttpsConfig httpsConfig) {
        if (httpsConfig.isEnabled()) {
            httpClient.secure(t -> t.sslContext(SslUtils.buildSSLHttpClient(
                    httpsConfig.getCert(),
                    httpsConfig.getKey(),
                    httpsConfig.isMutualAuthEnabled() ?
                            httpsConfig.getTrustCertificatesCollection()
                            : SslUtils.TRUST_ALL)));
        }
    }

    protected Mono<R> invokePdndRestService(Consumer<HttpHeaders> httpHeadersConsumer, T body, PdndInitiativeConfig pdndInitiativeConfig) {
        String bodyString = Utils.convertToJson(body, objectMapper);
        String digest = AgidUtils.buildDigest(bodyString);
        return retrievePdndAuthData(pdndInitiativeConfig)
                .flatMap(pdndAuthData -> AgidUtils.buildAgidJwtSignature(pdndServiceConfig, pdndInitiativeConfig, pdndAuthData.getJwtSignAlgorithm(), digest)
                        .flatMap(agidJwtSignature -> invokePdndRestService(pdndAuthData, httpHeadersConsumer, bodyString, digest, agidJwtSignature)));
    }

    private Mono<R> invokePdndRestService(PdndAuthData pdndAuthData, Consumer<HttpHeaders> httpHeadersConsumer, String bodyString, String digest, String agidJwtSignature) {
        return webClient.method(pdndServiceConfig.getHttpMethod())
                .uri(pdndServiceConfig.getPath())
                .headers(httpHeaders -> {
                    httpHeadersConsumer.accept(httpHeaders);
                    httpHeaders.setBearerAuth(pdndAuthData.getAccessToken());
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
                    httpHeaders.add("Digest", digest);
                    httpHeaders.add("Agid-JWT-TrackingEvidence", pdndAuthData.getAgidJwtTrackingEvidence());
                    httpHeaders.add("Agid-JWT-Signature", agidJwtSignature);
                })
                .bodyValue(bodyString)
                .retrieve()
                .bodyToMono(pdndServiceConfig.getResponseBodyClass())

                .doOnError(pdndServiceConfig.getTooManyRequestPredicate(), e -> {
                    throw new PdndServiceTooManyRequestException(pdndServiceConfig, e);
                })
                .onErrorResume(e -> {
                    log.error("[PDND] Something went wrong when invoking PDND service {}: {}", pdndServiceConfig.getAudience(), e.getMessage(), e);
                    return Mono.just(pdndServiceConfig.getEmptyResponseBody());
                });
    }
}
