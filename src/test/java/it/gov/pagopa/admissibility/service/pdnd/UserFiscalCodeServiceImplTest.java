package it.gov.pagopa.admissibility.service.pdnd;

import it.gov.pagopa.admissibility.dto.rest.UserInfoPDV;
import it.gov.pagopa.admissibility.rest.UserFiscalCodeRestClient;
import it.gov.pagopa.admissibility.utils.TestUtils;
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
        ReflectionUtils.setField(userCacheField, userFiscalCodeService,userCacheTest);
    }

    @Test
    void getUserInfoNotInCache(){
        // Given
        String userIdTest = "USERID_NEW";
        Mockito.when(userFiscalCodeRestClientMock.retrieveUserInfo(userIdTest)).thenReturn(Mono.just(UserInfoPDV.builder().pii("FISCALCODE_RETRIEVED").build()));

        // When
        Map<String, String> inspectCache = retrieveCache();
        Assertions.assertNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTest).block();


        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_RETRIEVED", result);
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache+1,inspectCache.size());


        Mockito.verify(userFiscalCodeRestClientMock).retrieveUserInfo(userIdTest);
    }

    @Test
    void getUserInfoInCache(){
        // Given
        String userIdTest = "USERID_0";

        // When
        Map<String, String> inspectCache = retrieveCache();
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        String result = userFiscalCodeService.getUserFiscalCode(userIdTest).block();

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_0", result);
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        Mockito.verify(userFiscalCodeRestClientMock, Mockito.never()).retrieveUserInfo(userIdTest);
    }

    private Map<String, String> retrieveCache() {
        Object cacheBefore = ReflectionUtils.getField(userCacheField, userFiscalCodeService);
        Assertions.assertNotNull(cacheBefore);
        return (Map<String, String>) cacheBefore;
    }
}