package it.gov.pagopa.admissibility.connector.rest.pdnd.services;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.connector.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.connector.rest.pdnd.PdndRestClient;
import it.gov.pagopa.admissibility.connector.rest.pdnd.components.JwtSignAlgorithmRetrieverService;
import it.gov.pagopa.admissibility.connector.rest.pdnd.config.PdndConfig;
import it.gov.pagopa.admissibility.connector.rest.pdnd.dto.PdndAuthData;
import it.gov.pagopa.admissibility.connector.rest.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.admissibility.connector.rest.pdnd.utils.AgidUtils;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public abstract class BasePdndService {
//TODO one concurrent request towards PDND per clientId

    private final ObjectMapper objectMapper;
    private final PdndServiceConfig pdndServiceConfig;
    private final PdndConfig pdndConfig;
    private final JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService;
    private final PdndRestClient pdndRestClient;

    private final Cache<PdndInitiativeConfig,PdndAuthData> pdndAuthDataCache;
    private final WebClient webClient;

    protected BasePdndService(
            PdndServiceConfig pdndServiceConfig,
            PdndConfig pdndConfig, JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient) {
        this.pdndServiceConfig = pdndServiceConfig;
        this.pdndConfig = pdndConfig;
        this.jwtSignAlgorithmRetrieverService = jwtSignAlgorithmRetrieverService;
        this.pdndRestClient = pdndRestClient;

        if (pdndServiceConfig.getAuthExpirationSeconds()!=0){
            pdndAuthDataCache = CacheBuilder.newBuilder().expireAfterWrite(pdndServiceConfig.getAuthExpirationSeconds(), TimeUnit.SECONDS).build();
        }
        else {
            pdndAuthDataCache = null;
        }
    }

    protected <T, R> Mono<R> invokePdndService(HttpMethod httpMethod, String path, Consumer<HttpHeaders> httpHeadersConsumer, T body, Class<R> responseBodyClass, PdndInitiativeConfig pdndInitiativeConfig){
        String bodyString = Utils.convertToJson(body, objectMapper);
        String digest = AgidUtils.buildDigest(bodyString);
        return retrievePdndAuthData(pdndInitiativeConfig)
                .flatMap(pdndAuthData -> AgidUtils.buildAgidJwtSignature(pdndServiceConfig, pdndInitiativeConfig, pdndAuthData.getJwtSignAlgorithm(), digest)
                        .flatMap(agidJwtSignature -> invokePdndService(pdndAuthData, httpMethod, path, httpHeadersConsumer, bodyString, digest, agidJwtSignature, responseBodyClass)));
    }

    private <R> Mono<R> invokePdndService(PdndAuthData pdndAuthData, HttpMethod httpMethod, String path, Consumer<HttpHeaders> httpHeadersConsumer, String bodyString, String digest, String agidJwtSignature, Class<R> responseBodyClass) {
        return webClient.method(httpMethod)
                .uri(path)
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
                .bodyToMono(responseBodyClass)

                //TODO define error code for retry
                .doOnError(WebClientResponseException.TooManyRequests.class, e -> {
                    throw new AnprDailyRequestLimitException(e);
                })
                .onErrorResume(e -> {
                    log.error("Something went wrong when invoking PDND service : {}", e.getMessage(), e);
                    return Mono.just(responseBodyClass.getConstructor().newInstance());
                });
    }

    protected Mono<PdndAuthData> retrievePdndAuthData(PdndInitiativeConfig pdndInitiativeConfig){
        if(pdndAuthDataCache != null){
            PdndAuthData pdndAuthData = pdndAuthDataCache.getIfPresent(pdndInitiativeConfig);
            if(pdndAuthData != null) {
                log.debug("[CACHE_HIT][PDND] Retrieving PDND auth data for {}", pdndInitiativeConfig);
                return Mono.just(pdndAuthData);
            } else {
                log.info("[CACHE_MISS][PDND] Retrieving PDND auth data for {}", pdndInitiativeConfig);
                return retrievePdndAuthDataInner(pdndInitiativeConfig)
                        .doOnNext(authData -> pdndAuthDataCache.put(pdndInitiativeConfig,authData));
            }
        }else {
            log.info("[PDND] Retrieving PDND auth data for {}", pdndInitiativeConfig);
            return retrievePdndAuthDataInner(pdndInitiativeConfig);
        }
    }

    private Mono<PdndAuthData> retrievePdndAuthDataInner(PdndInitiativeConfig pdndInitiativeConfig) {
        return invokePdnd(pdndInitiativeConfig, jwtSignAlgorithmRetrieverService.retrieve(pdndInitiativeConfig));
    }

    private Mono<PdndAuthData> invokePdnd(PdndInitiativeConfig pdndInitiativeConfig, Algorithm jwtSignAlgorithm) {
        return AgidUtils.preparePdndAuthData2invokePdnd(pdndServiceConfig, pdndConfig, pdndInitiativeConfig, jwtSignAlgorithm)
                        .flatMap(pdndAuthData -> pdndRestClient.createToken(pdndInitiativeConfig.getClientId(), pdndAuthData.getClientAssertion())
                                .map(pdndToken -> {
                                    pdndAuthData.setAccessToken(pdndToken.getAccessToken());
                                    return pdndAuthData;
                                }));
    }


}
