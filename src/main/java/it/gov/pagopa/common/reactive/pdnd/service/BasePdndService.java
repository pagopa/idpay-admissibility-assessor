package it.gov.pagopa.common.reactive.pdnd.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdnd.components.JwtSignAlgorithmRetrieverService;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceProviderConfig;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndAuthData;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.utils.AgidUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class BasePdndService<R, S> {

    protected final ObjectMapper objectMapper;
    protected final PdndServiceConfig<R, S> pdndServiceConfig;
    protected final PdndConfig pdndConfig;
    private final JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService;
    private final PdndRestClient pdndRestClient;

    private final Cache<PdndInitiativeConfig, PdndAuthData> pdndAuthDataCache;

    protected BasePdndService(
            PdndServiceConfig<R, S> pdndServiceConfig,
            ObjectMapper objectMapper, PdndConfig pdndConfig, JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient) {
        this.objectMapper = objectMapper;
        this.pdndServiceConfig = pdndServiceConfig;
        this.pdndConfig = pdndConfig;
        this.jwtSignAlgorithmRetrieverService = jwtSignAlgorithmRetrieverService;
        this.pdndRestClient = pdndRestClient;

        if (pdndServiceConfig.getAuthExpirationSeconds() != 0) {
            pdndAuthDataCache = CacheBuilder.newBuilder().expireAfterWrite(pdndServiceConfig.getAuthExpirationSeconds(), TimeUnit.SECONDS).build();
        } else {
            pdndAuthDataCache = null;
        }
    }

    protected Mono<PdndAuthData> retrievePdndAuthData(PdndInitiativeConfig pdndInitiativeConfig) {
        if (pdndAuthDataCache != null) {
            PdndAuthData pdndAuthData = pdndAuthDataCache.getIfPresent(pdndInitiativeConfig);
            if (pdndAuthData != null) {
                log.debug("[CACHE_HIT][PDND] Retrieved cached PDND auth data for {}", pdndInitiativeConfig);
                return Mono.just(pdndAuthData);
            } else {
                // Consider to use it.gov.pagopa.common.reactive.service.LockService if we want just one concurrent PDND invoke per pdndInitiativeConfig
                log.info("[CACHE_MISS][PDND] Retrieving PDND auth data for {}", pdndInitiativeConfig);
                return retrievePdndAuthDataInner(pdndInitiativeConfig)
                        .doOnNext(authData -> pdndAuthDataCache.put(pdndInitiativeConfig, authData));
            }
        } else {
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

    public static <R, E> PdndServiceConfig<R, E> buildDefaultPdndServiceConfig(BasePdndServiceProviderConfig providerConfig, BasePdndServiceConfig serviceConfig, Class<R> responseBodyClass, Class<E> responseErrorBodyClass) {
        PdndServiceConfig<R, E> out = new PdndServiceConfig<>();

        // provider config
        out.setBaseUrl(providerConfig.getBaseUrl());
        out.setAuthExpirationSeconds(providerConfig.getAuthExpirationSeconds());
        out.setHttpsConfig(providerConfig.getHttpsConfig());
        out.setAgidConfig(providerConfig.getAgidConfig());

        // service config
        out.setAudience(serviceConfig.getAudience());
        out.setHttpMethod(serviceConfig.getHttpMethod());
        out.setPath(serviceConfig.getPath());

        out.setResponseBodyClass(responseBodyClass);
        out.setResponseErrorBodyClass(responseErrorBodyClass);

        return out;
    }
}
