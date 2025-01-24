package it.gov.pagopa.common.reactive.pdv.service;

import com.google.common.cache.Cache;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFiscalCodeServiceImplTest {

    @Mock
    private UserFiscalCodeRestClient userFiscalCodeRestClientMock;

    private UserFiscalCodeService userFiscalCodeService;

    private final int initialSizeCache = 2;
    private Field userCacheField;


    final String  userIdTest = "USERID_0";
    final String cf = "FISCALCODE_0";

    @BeforeEach
    void setUp() {
        userFiscalCodeService = new UserFiscalCodeServiceImpl(userFiscalCodeRestClientMock);

        Map<String, String> userCacheTest = new ConcurrentHashMap<>();
        IntStream.range(0, initialSizeCache).forEach(i -> userCacheTest.put("USERID_%d".formatted(i),
                "FISCALCODE_%d".formatted(i)));

        userCacheField = ReflectionUtils.findField(UserFiscalCodeServiceImpl.class, "userCache");
        Assertions.assertNotNull(userCacheField);
        ReflectionUtils.makeAccessible(userCacheField);
        @SuppressWarnings("unchecked") Cache<String, String> cache = (Cache<String, String>)ReflectionUtils.getField(userCacheField, userFiscalCodeService);
        Assertions.assertNotNull(cache);
        cache.invalidateAll();
        cache.putAll(userCacheTest);

    }

    @Test
    void getUserInfoNotInCache(){
        // Given
        final String userIdTestNotInCache = "USERID_NEW";
        final String cfNotInCache = "CF_NEW";
        Mockito.when(userFiscalCodeRestClientMock.retrieveUserInfo(userIdTestNotInCache)).thenReturn(Mono.just(UserInfoPDV.builder().pii(cfNotInCache).build()));

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNull(inspectCache.getIfPresent(userIdTestNotInCache));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTestNotInCache).block();


        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(cfNotInCache, result);
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTestNotInCache));
        Assertions.assertEquals(initialSizeCache+1,inspectCache.size());


        Mockito.verify(userFiscalCodeRestClientMock).retrieveUserInfo(userIdTestNotInCache);
    }

    @Test
    void getUserInfoInCache(){
        // Given

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTest).block();
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("FISCALCODE_0", result);
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        Mockito.verify(userFiscalCodeRestClientMock, Mockito.never()).retrieveUserInfo(userIdTest);
    }

    private Cache<String, String> retrieveCache() {
        Object cacheBefore = ReflectionUtils.getField(userCacheField, userFiscalCodeService);
        Assertions.assertNotNull(cacheBefore);
        //noinspection unchecked
        return (Cache<String, String>) cacheBefore;
    }

    @Test
    void getUserIdNotInCache(){
        // Given
        when(userFiscalCodeRestClientMock.retrieveUserId(cf)).thenReturn(Mono.just(UserIdPDV.builder().token(userIdTest).build()));

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNull(inspectCache.getIfPresent(cf));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserId(cf).block();


        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(userIdTest, result);
        Assertions.assertEquals(initialSizeCache+1,inspectCache.size());


        Mockito.verify(userFiscalCodeRestClientMock).retrieveUserId(cf);
    }

    @Test
    void getUserIdInCache(){
        // Given

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserId(userIdTest).block();
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("FISCALCODE_0", result);
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        Mockito.verify(userFiscalCodeRestClientMock, Mockito.never()).retrieveUserInfo(userIdTest);
    }

}