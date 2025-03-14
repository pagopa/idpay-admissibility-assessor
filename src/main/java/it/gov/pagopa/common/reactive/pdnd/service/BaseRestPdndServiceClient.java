package it.gov.pagopa.common.reactive.pdnd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.http.utils.NettySslUtils;
import it.gov.pagopa.common.reactive.pdnd.components.JwtSignAlgorithmRetrieverService;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceProviderConfig;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndAuthData;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import it.gov.pagopa.common.reactive.pdnd.utils.AgidUtils;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.utils.CommonUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

        httpClient = configureHttps(httpClient, pdndServiceConfig.getHttpsConfig());

        this.webClient = webClientBuilder.clone()
                .baseUrl(pdndServiceConfig.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static HttpClient configureHttps(HttpClient httpClient, BasePdndServiceProviderConfig.HttpsConfig httpsConfig) {
        String trustCertCollectionString = httpsConfig.isMutualAuthEnabled() ?
                httpsConfig.getTrustCertificatesCollection()
                : NettySslUtils.TRUST_ALL;

        if (httpsConfig.isEnabled()) {
            return httpClient.secure(t -> t.sslContext(NettySslUtils.buildSSLContext(
                    httpsConfig.getCert(),
                    httpsConfig.getKey(),
                    trustCertCollectionString)));
        } else {
            return httpClient.secure(t -> t.sslContext(NettySslUtils.buildSSLContext(trustCertCollectionString)));
        }
    }

    protected Mono<R> invokePdndRestService(Consumer<HttpHeaders> httpHeadersConsumer, T body, PdndInitiativeConfig pdndInitiativeConfig) {
        String bodyString = CommonUtilities.convertToJson(body, objectMapper);
        String digest = AgidUtils.buildDigest(bodyString);
        return retrievePdndAuthData(pdndInitiativeConfig)
                .flatMap(pdndAuthData -> AgidUtils.buildAgidJwtSignature(pdndServiceConfig, pdndInitiativeConfig, pdndAuthData.getJwtSignAlgorithm(), digest)
                        .flatMap(agidJwtSignature -> invokePdndRestService(pdndAuthData, httpHeadersConsumer, bodyString, digest, agidJwtSignature)));
    }

    private Mono<R> invokePdndRestService(PdndAuthData pdndAuthData, Consumer<HttpHeaders> httpHeadersConsumer, String bodyString, String digest, String agidJwtSignature) {

        return PerformanceLogger.logTimingOnNext(
                "PDND_SERVICE_INVOKE",
                webClient.method(pdndServiceConfig.getHttpMethod())
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

                        .onErrorResume(e -> {
                            if(e instanceof WebClientResponseException.NotFound notFoundException){
                                log.error("[PDND_SERVICE_INVOKE] Cannot found data when invoking PDND service {}: {}", pdndServiceConfig.getAudience(), notFoundException.getResponseBodyAsString());
                                return Mono.just(pdndServiceConfig.getEmptyResponseBody());
                            } else if(pdndServiceConfig.getTooManyRequestPredicate().test(e)){
                                return Mono.error(new PdndServiceTooManyRequestException(pdndServiceConfig, e));
                            }
                            else {
                                return Mono.error(new IllegalStateException("[PDND_SERVICE_INVOKE] Something went wrong when invoking PDND service %s: %s".formatted(pdndServiceConfig.getAudience(), e.getMessage()), e));
                            }
                        }),
                x -> "[" + getClass().getSimpleName() + "] "
        );
    }
}
