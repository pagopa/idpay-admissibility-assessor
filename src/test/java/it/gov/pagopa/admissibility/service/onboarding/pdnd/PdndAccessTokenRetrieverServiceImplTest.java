package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.gov.pagopa.admissibility.connector.rest.PdndCreateTokenRestClient;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.common.utils.TestUtils;
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
class PdndAccessTokenRetrieverServiceImplTest {

    @Mock
    private PdndCreateTokenRestClient pdndCreateTokenRestClientMock;

    private PdndAccessTokenRetrieverService pdndAccessTokenRetrieverService;

    private Field accessTokenCacheField;

    private final int expireInSeconds = 5;

    @BeforeEach
    void setUp() {
        pdndAccessTokenRetrieverService = new PdndAccessTokenRetrieverServiceImpl(pdndCreateTokenRestClientMock, expireInSeconds);

        ApiKeysPDND apiKeysPDND = ApiKeysPDND.builder()
                .apiKeyClientId("API_KEY_CLIENT_ID")
                .apiKeyClientAssertion("API_KEY_CLIENT_ASSERTION")
                .build();

        Cache<ApiKeysPDND,String> cacheTest = CacheBuilder.newBuilder().expireAfterAccess(expireInSeconds, TimeUnit.SECONDS).build();
        cacheTest.put(apiKeysPDND,"accessToken_1");

        accessTokenCacheField = ReflectionUtils.findField(PdndAccessTokenRetrieverServiceImpl.class, "accessTokenCache");
        Assertions.assertNotNull(accessTokenCacheField);
        ReflectionUtils.makeAccessible(accessTokenCacheField);
        ReflectionUtils.setField(accessTokenCacheField, pdndAccessTokenRetrieverService, cacheTest);
    }

    @Test
    void getTokenPresentInCache() {
        // Given
        ApiKeysPDND apiKeysPDND_given = ApiKeysPDND.builder()
                .apiKeyClientId("API_KEY_CLIENT_ID")
                .apiKeyClientAssertion("API_KEY_CLIENT_ASSERTION")
                .build();

        // When
        String result = pdndAccessTokenRetrieverService.getToken(apiKeysPDND_given).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("accessToken_1", result);

        int deltaMillis = 10;
        TestUtils.wait((expireInSeconds*1000L)+ deltaMillis, TimeUnit.MILLISECONDS);

        Cache<String, String> cacheChange = retrieveCache();
        //noinspection SuspiciousMethodCalls
        Assertions.assertNull(cacheChange.getIfPresent(apiKeysPDND_given));
        Mockito.verify(pdndCreateTokenRestClientMock, Mockito.never()).createToken(apiKeysPDND_given);
    }

    @Test
    void getTokenNotInCache() {
        //Given
        ApiKeysPDND apiKeysPDND_NEW = ApiKeysPDND.builder()
                .apiKeyClientId("API_KEY_CLIENT_ID_NEW")
                .apiKeyClientAssertion("API_KEY_CLIENT_ASSERTION_NEW")
                .build();
        String expectedToken = "NEW_ACCESS_TOKEN";

        ClientCredentialsResponseDTO clientCredentialsResponseDTOMock = new ClientCredentialsResponseDTO();
        clientCredentialsResponseDTOMock.setAccessToken(expectedToken);
        Mockito.when(pdndCreateTokenRestClientMock.createToken(apiKeysPDND_NEW))
                .thenReturn(Mono.just(clientCredentialsResponseDTOMock));

        // When
        String result = pdndAccessTokenRetrieverService.getToken(apiKeysPDND_NEW).block();

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals("NEW_ACCESS_TOKEN", result);

        Mockito.verify(pdndCreateTokenRestClientMock).createToken(apiKeysPDND_NEW);

        Cache<String, String> cacheChange = retrieveCache();
        //noinspection SuspiciousMethodCalls
        String retrievedTokenInCache = cacheChange.getIfPresent(apiKeysPDND_NEW);
        Assertions.assertNotNull(retrievedTokenInCache);
        Assertions.assertEquals("NEW_ACCESS_TOKEN", retrievedTokenInCache);
    }

    @Test
    void getTokenPresentWithoutCache() {
        // Given
        pdndAccessTokenRetrieverService = new PdndAccessTokenRetrieverServiceImpl(pdndCreateTokenRestClientMock, 0);
        ApiKeysPDND apiKeysPDND1 = ApiKeysPDND.builder()
                .apiKeyClientId("API_KEY_CLIENT_ID_1")
                .apiKeyClientAssertion("API_KEY_CLIENT_ASSERTION_1")
                .build();

        String expectedToken = "NEW_ACCESS_TOKEN";
        ClientCredentialsResponseDTO clientCredentialsResponseDTOMock = new ClientCredentialsResponseDTO();
        clientCredentialsResponseDTOMock.setAccessToken(expectedToken);
        Mockito.when(pdndCreateTokenRestClientMock.createToken(apiKeysPDND1))
                .thenReturn(Mono.just(clientCredentialsResponseDTOMock));

        // When
        String result = pdndAccessTokenRetrieverService.getToken(apiKeysPDND1).block();

        //Then
        Field accessTokenCacheNullField = ReflectionUtils.findField(PdndAccessTokenRetrieverServiceImpl.class, "accessTokenCache");
        Assertions.assertNotNull(accessTokenCacheNullField);
        ReflectionUtils.makeAccessible(accessTokenCacheNullField);
        Object cacheInspect = ReflectionUtils.getField(accessTokenCacheNullField, pdndAccessTokenRetrieverService);
        Assertions.assertNull(cacheInspect);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("NEW_ACCESS_TOKEN", result);

        Mockito.verify(pdndCreateTokenRestClientMock).createToken(apiKeysPDND1);
    }

    private Cache<String,String> retrieveCache() {
        Object cacheBefore = ReflectionUtils.getField(accessTokenCacheField, pdndAccessTokenRetrieverService);
        Assertions.assertNotNull(cacheBefore);
        return (Cache<String,String>) cacheBefore;
    }
}