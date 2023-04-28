package it.gov.pagopa.admissibility.service.pdnd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.admissibility.connector.rest.PdndCreateTokenRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class CreateTokenServiceImpl implements CreateTokenService{

    private final PdndCreateTokenRestClient pdndCreateTokenRestClient;
    private final Cache<ApiKeysPDND,String> accessTokenCache;

    public CreateTokenServiceImpl(PdndCreateTokenRestClient pdndCreateTokenRestClient,
                                  @Value("${app.pdnd.time-expire-token}") int expireIn) {
        this.pdndCreateTokenRestClient = pdndCreateTokenRestClient;
        if (expireIn!=0){
            accessTokenCache = CacheBuilder.newBuilder().expireAfterWrite(expireIn, TimeUnit.SECONDS).build();
        }
        else {
            accessTokenCache = null;
        }
    }

    @Override
    public Mono<String> getToken(ApiKeysPDND apiKeysPDND) {
        if(accessTokenCache != null){
            String accessToken = accessTokenCache.getIfPresent(apiKeysPDND);
            if(accessToken != null) {
                return Mono.just(accessToken);
            } else {
                return retrieveAccessToken(apiKeysPDND)
                        .doOnNext(token -> accessTokenCache.put(apiKeysPDND,token));
            }
        }else {
            return retrieveAccessToken(apiKeysPDND);
        }
    }

    private Mono<String> retrieveAccessToken(ApiKeysPDND apiKeysPDND){
        return pdndCreateTokenRestClient.createToken(apiKeysPDND)
                .map(ClientCredentialsResponseDTO::getAccessToken);
    }
}