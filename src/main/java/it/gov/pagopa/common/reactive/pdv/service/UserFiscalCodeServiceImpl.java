package it.gov.pagopa.common.reactive.pdv.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserFiscalCodeServiceImpl implements UserFiscalCodeService{
    private final Cache<String, String> userCache;
    private final UserFiscalCodeRestClient userFiscalCodeRestClient;

    public UserFiscalCodeServiceImpl(UserFiscalCodeRestClient userRestClient) {
        this.userFiscalCodeRestClient = userRestClient;

        // Maximum size ca 256MB
        this.userCache = CacheBuilder.newBuilder().maximumSize(8_388_000L).build();
    }

    @Override
    public Mono<String> getUserFiscalCode(String userId) {
        String userFromCache = userCache.getIfPresent(userId);
        if(userFromCache != null){
            return Mono.just(userFromCache);
        }else {
            return userFiscalCodeRestClient.retrieveUserInfo(userId)
                    .map(UserInfoPDV::getPii)
                    .doOnNext(u -> {
                        userCache.put(userId,u);
                        log.debug("[CACHE_MISS] Added into map user fiscal code with userId: {}", userId);
                    });
        }
    }

    @Override
    public Mono<String> getUserId(String fiscalCode) {
        String userFromCache = userCache.getIfPresent(fiscalCode);
        if(userFromCache != null){
            return Mono.just(userFromCache);
        }else {
            return userFiscalCodeRestClient.retrieveUserId(fiscalCode)
                    .map(UserIdPDV::getToken)
                    .doOnNext(u -> {
                        userCache.put(fiscalCode,u);
                        log.debug("[CACHE_MISS] Added into map userId with fiscalCode: {}", fiscalCode);
                    });
        }
    }


}