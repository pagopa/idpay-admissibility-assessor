package it.gov.pagopa.admissibility.connector.rest.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesReqDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResElemDTO;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(
        classes = {
                OnboardingRestClientImpl.class,
                WebClientConfig.class
        })
@TestPropertySource(
        properties = {
                WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX + "app.onboarding-workflow.base-url=onboardingMock"
        }
)
@SpringBootTest
class OnboardingRestClientImplTest extends BaseWireMockTest {

    private static final String INITIATIVE_ID = "initiativeId_1";

    @Autowired
    private OnboardingRestClientImpl client;


    @Test
    void testAlreadyOnboardingStatus_okStatusWithOnboardingOk() {
        Pair<Boolean, String> result = client.alreadyOnboardingStatus(INITIATIVE_ID, "userId_onboardingOK").block();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getKey());
        Assertions.assertEquals("userId_onboardingOK", result.getValue());
    }

    @Test
    void testAlreadyOnboardingStatus_okStatusWithJoined() {
        Pair<Boolean, String> result = client.alreadyOnboardingStatus(INITIATIVE_ID, "userId_JOINED").block();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getKey());
        Assertions.assertNull(result.getValue());
    }

    @Test
    void testAlreadyOnboardingStatus_okStatusWithNotOnboarded() {
        Pair<Boolean, String> result = client.alreadyOnboardingStatus(INITIATIVE_ID, "userId_NotOnboarded").block();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getKey());
        Assertions.assertNull(result.getValue());
    }

    @Test
    void testAlreadyOnboardingStatus_tooManyRequestException(){
        try{
            client.alreadyOnboardingStatus(INITIATIVE_ID, "userId_TooManyRequest").block();
            Assertions.fail();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }

    @Test
    void testCheckPastOnboardingsSeries_returnsResponseAndSendsRequest() {
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        CheckPastOnboardingsSeriesResDTO expectedResponse = new CheckPastOnboardingsSeriesResDTO(List.of(
                new CheckPastOnboardingsSeriesResElemDTO("userId_1", true),
                new CheckPastOnboardingsSeriesResElemDTO("userId_2", false)));

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(
                eq("/idpay/onboarding/{initiativeId}/check-past-onboardings-series"),
                anyMap())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(CheckPastOnboardingsSeriesResDTO.class))
                .thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));

        OnboardingRestClientImpl unitUnderTest = new OnboardingRestClientImpl(
                "http://onboarding-workflow",
                1,
                1,
                webClientBuilder);
        CheckPastOnboardingsSeriesReqDTO request =
                new CheckPastOnboardingsSeriesReqDTO(List.of("userId_1", "userId_2"));

        CheckPastOnboardingsSeriesResDTO result = unitUnderTest.checkPastOnboardingsSeries(
                INITIATIVE_ID, request)
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResponse, result);
        verify(requestBodyUriSpec).uri(
                "/idpay/onboarding/{initiativeId}/check-past-onboardings-series",
                Map.of("initiativeId", INITIATIVE_ID));
        verify(requestBodySpec).bodyValue(request);
    }

}
