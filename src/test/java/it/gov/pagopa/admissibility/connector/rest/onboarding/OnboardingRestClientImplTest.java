package it.gov.pagopa.admissibility.connector.rest.onboarding;

import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

import static it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;

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

}
