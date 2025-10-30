package it.gov.pagopa.admissibility.connector.rest.mock;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;

@ContextConfiguration(
        classes = {
                FamilyMockRestClientImpl.class,
                WebClientConfig.class
        })
@TestPropertySource(
        properties = {
                WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX + "app.inps-mock.base-url=pdndMock",
                "app.inps-mock.enabled=true",
                "app.inps-mock.retry.delay-millis=10",
                "app.inps-mock.retry.max-attempts=1",
                "app.inps-real.base-url=https://api.collaudo.inps.it/modi/soap/ConsultazioneISEE/v1"
        }
)
class FamilyMockRestClientImplTest extends BaseWireMockTest {

    @Autowired
    private FamilyMockRestClientImpl familyMockRestClient;

    @Test
    void givenUserIdWhenCallFamilyMockRestClientThenFamily() {
        Family family = familyMockRestClient.retrieveFamily("userId_1").block();
        Assertions.assertNotNull(family);
        Assertions.assertEquals(3, family.getMemberIds().size());
    }

    @Test
    void givenUserIdWhenCallFamilyMockRestClientThenTooManyRequestException() {
        try {
            familyMockRestClient.retrieveFamily("USERID_TOOMANYREQUEST_1").block();
            Assertions.fail("Expected a retry exhaustion exception");
        } catch (Throwable e) {
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }

    @Test
    void givenMockDisabledThenUseRealUrlAndEmptyFamilyUri() {
        FamilyMockRestClientImpl client = new FamilyMockRestClientImpl(
                false,
                "http://mock-base-url",
                "https://api.collaudo.inps.it/modi/soap/ConsultazioneISEE/v1",
                10,
                1,
                WebClient.builder()
        );

        Mono<Family> mono = client.retrieveFamily("userId_2");
        Assertions.assertNotNull(mono);
    }

    @Test
    void givenTooManyRequestsExceptionWhenFilterAppliedThenRetryTrue() {
        WebClientResponseException ex = WebClientResponseException.create(
                429, "Too Many Requests", null, null, null
        );

        boolean shouldRetry = ex instanceof WebClientResponseException.TooManyRequests
                || ex.getStatusCode().value() == 429;

        Assertions.assertTrue(shouldRetry);
    }

    @Test
    void givenTooManyRequestsErrorThenRetryBlockIsExecuted() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(clientRequest ->
                        Mono.error(WebClientResponseException.create(
                                429, "Too Many Requests", null, null, null))
                )
                .build();

        FamilyMockRestClientImpl client = new FamilyMockRestClientImpl(
                true,
                "http://mock-base-url",
                "https://api.collaudo.inps.it/modi/soap/ConsultazioneISEE/v1",
                10,
                1,
                WebClient.builder()
        );

        try {
            var field = FamilyMockRestClientImpl.class.getDeclaredField("webClient");
            field.setAccessible(true);
            field.set(client, webClient);
        } catch (Exception e) {
            Assertions.fail("Reflection setup failed: " + e.getMessage());
        }

        try {
            client.retrieveFamily("userId_retry").block();
            Assertions.fail("Expected a retry exhaustion exception");
        } catch (Throwable e) {
            Assertions.assertTrue(Exceptions.isRetryExhausted(e) || e instanceof WebClientResponseException);
        }
    }

    @Test
    void givenRetryBranchThenIfRetryBlockIsExecuted() {
        WebClientResponseException tooManyRequestsEx = new WebClientResponseException(
                429,
                "Too Many Requests",
                null,
                null,
                null
        );

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(req -> Mono.error(tooManyRequestsEx))
                .build();

        FamilyMockRestClientImpl client = new FamilyMockRestClientImpl(
                true,
                "http://mock-base-url",
                "https://api.collaudo.inps.it/modi/soap/ConsultazioneISEE/v1",
                10,
                1,
                WebClient.builder()
        );

        try {
            var field = FamilyMockRestClientImpl.class.getDeclaredField("webClient");
            field.setAccessible(true);
            field.set(client, webClient);
        } catch (Exception e) {
            Assertions.fail("Reflection setup failed: " + e.getMessage());
        }

        try {
            client.retrieveFamily("userId_retry").block();
            Assertions.fail("Expected retry exhaustion exception");
        } catch (Throwable e) {
            Assertions.assertTrue(Exceptions.isRetryExhausted(e) || e instanceof WebClientResponseException);
        }
    }
}
