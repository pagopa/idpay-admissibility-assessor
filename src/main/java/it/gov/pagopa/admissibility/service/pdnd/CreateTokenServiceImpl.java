package it.gov.pagopa.admissibility.service.pdnd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.admissibility.rest.PdndCreateTokenRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class CreateTokenServiceImpl implements CreateTokenService{

    private final PdndCreateTokenRestClient pdndCreateTokenRestClient;
    private Cache<String,String> cache = null;

    public CreateTokenServiceImpl(PdndCreateTokenRestClient pdndCreateTokenRestClient,
                                  @Value("${app.pdnd.time-expire-token}") int expireIn) {
        this.pdndCreateTokenRestClient = pdndCreateTokenRestClient;
        if (expireIn!=0){
            cache = CacheBuilder.newBuilder().expireAfterWrite(expireIn, TimeUnit.SECONDS).build();
        }
    }

    @Override
    public Mono<String> getToken(String pdndToken) {
        if(cache != null){
            String accessToken = cache.getIfPresent(pdndToken);
            if(accessToken != null) {
                return Mono.just(accessToken);
            } else {
                return retrieveAccessToken(pdndToken)
                        .doOnNext(token -> cache.put(pdndToken,token));
            }
        }else {
            return retrieveAccessToken(pdndToken);
        }
    }

    private Mono<String> retrieveAccessToken(String pdndToken){
        return pdndCreateTokenRestClient.createToken(pdndToken)
                .map(ClientCredentialsResponseDTO::getAccessToken);
    }
}