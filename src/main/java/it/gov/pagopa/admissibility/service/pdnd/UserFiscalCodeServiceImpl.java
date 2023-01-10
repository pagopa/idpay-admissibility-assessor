package it.gov.pagopa.admissibility.service.pdnd;

import it.gov.pagopa.admissibility.dto.rest.UserInfoPDV;
import it.gov.pagopa.admissibility.rest.UserFiscalCodeRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserFiscalCodeServiceImpl implements UserFiscalCodeService{
    private final Map<String, String> userCache = new ConcurrentHashMap<>();
    private final UserFiscalCodeRestClient userFiscalCodeRestClient;

    public UserFiscalCodeServiceImpl(UserFiscalCodeRestClient userRestClient) {
        this.userFiscalCodeRestClient = userRestClient;
    }
    @Override
    public Mono<String> getUserFiscalCode(String userId) {
        String userFromCache = userCache.get(userId);
        if(userFromCache != null){
            return Mono.just(userFromCache);
        }else {
            return userFiscalCodeRestClient.retrieveUserInfo(userId)
                    .map(UserInfoPDV::getPii)
                    .doOnNext(u -> {
                        userCache.put(userId,u);
                        log.info("Added into map user fiscal code with userId: {}", userId);
                    });
        }
    }
}