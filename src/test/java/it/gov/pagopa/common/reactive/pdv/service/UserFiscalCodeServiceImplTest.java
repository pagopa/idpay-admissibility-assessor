package it.gov.pagopa.common.reactive.pdv.service;

import com.google.common.cache.Cache;
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

@ExtendWith(MockitoExtension.class)
class UserFiscalCodeServiceImplTest {

    @Mock
    private UserFiscalCodeRestClient userFiscalCodeRestClientMock;

    private UserFiscalCodeService userFiscalCodeService;

    private final int initialSizeCache = 2;
    private Field userCacheField;

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
        String userIdTest = "USERID_NEW";
        Mockito.when(userFiscalCodeRestClientMock.retrieveUserInfo(userIdTest)).thenReturn(Mono.just(UserInfoPDV.builder().pii("FISCALCODE_RETRIEVED").build()));

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTest).block();


        // Then
        Assertions.assertNotNull(result);
        //TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_RETRIEVED", result);
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache+1,inspectCache.size());


        Mockito.verify(userFiscalCodeRestClientMock).retrieveUserInfo(userIdTest);
    }

    @Test
    void getUserInfoInCache(){
        // Given
        String userIdTest = "USERID_0";

        // When
        Cache<String, String> inspectCache = retrieveCache();
        Assertions.assertNotNull(inspectCache.getIfPresent(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTest).block();
        // Then
        Assertions.assertNotNull(result);
        //TestUtils.checkNotNullFields(result);
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
}