package it.gov.pagopa.admissibility.service.pdnd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.admissibility.rest.PdndCreateTokenRestClient;
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
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class CreateTokenServiceImplTest {

    @Mock
    private PdndCreateTokenRestClient pdndCreateTokenRestClientMock;

    private CreateTokenService createTokenService;

    private Field accessTokenCacheField;

    private final int expireInSeconds = 30;
    private final int deltaMillis = 10;

    @BeforeEach
    void setUp() {
        createTokenService = new CreateTokenServiceImpl(pdndCreateTokenRestClientMock, expireInSeconds);


        Cache<String,String> cacheTest = CacheBuilder.newBuilder().expireAfterAccess(expireInSeconds, TimeUnit.SECONDS).build();
        cacheTest.put("pdndToken_1","accessToken_1");

        accessTokenCacheField = ReflectionUtils.findField(CreateTokenServiceImpl.class, "cache");
        Assertions.assertNotNull(accessTokenCacheField);
        ReflectionUtils.makeAccessible(accessTokenCacheField);
        ReflectionUtils.setField(accessTokenCacheField, createTokenService, cacheTest);
    }

    @Test
    void getTokenPresentInCache() {
        // Given
        String pdndTokenTest = "pdndToken_1";

        // When
        String result = createTokenService.getToken(pdndTokenTest).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("accessToken_1", result);

        BaseIntegrationTest.wait((expireInSeconds*1000L)+deltaMillis, TimeUnit.MILLISECONDS);

        Cache<String, String> cacheChange = retrieveCache();
        Assertions.assertNull(cacheChange.getIfPresent(pdndTokenTest));
        Mockito.verify(pdndCreateTokenRestClientMock, Mockito.never()).createToken(pdndTokenTest);
    }

    @Test
    void getTokenNotInCache() {
        //Given
        String pdndTokenTest = "pdndToken_new";
        String expectedToken = "NEW_ACCESS_TOKEN";

        ClientCredentialsResponseDTO clientCredentialsResponseDTOMock = new ClientCredentialsResponseDTO();
        clientCredentialsResponseDTOMock.setAccessToken(expectedToken);
        Mockito.when(pdndCreateTokenRestClientMock.createToken(pdndTokenTest))
                .thenReturn(Mono.just(clientCredentialsResponseDTOMock));

        // When
        String result = createTokenService.getToken(pdndTokenTest).block();

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals("NEW_ACCESS_TOKEN", result);

        Mockito.verify(pdndCreateTokenRestClientMock).createToken(pdndTokenTest);

        Cache<String, String> cacheChange = retrieveCache();
        String retrievedTokenInCache = cacheChange.getIfPresent(pdndTokenTest);
        Assertions.assertNotNull(retrievedTokenInCache);
        Assertions.assertEquals("NEW_ACCESS_TOKEN", retrievedTokenInCache);
    }

    private Cache<String,String> retrieveCache() {
        Object cacheBefore = ReflectionUtils.getField(accessTokenCacheField, createTokenService);
        Assertions.assertNotNull(cacheBefore);
        return (Cache<String,String>) cacheBefore;
    }
}