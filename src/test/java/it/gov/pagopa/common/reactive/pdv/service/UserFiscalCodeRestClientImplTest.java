package it.gov.pagopa.common.reactive.pdv.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test integrato del client UserFiscalCodeRestClientImpl senza AutoConfigureWireMock
 */
@SpringBootTest(classes = {UserFiscalCodeRestClientImpl.class, WebClientConfig.class, ObjectMapper.class})
class UserFiscalCodeRestClientImplTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.pdv.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/pdv");
        registry.add("app.pdv.retry.delay-millis", () -> "100");
        registry.add("app.pdv.retry.max-attempts", () -> "1");
        registry.add("app.pdv.headers.x-api-key", () -> "x_api_key");
    }

    @BeforeEach
    void setupWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void retrieveUserInfoOk() {
        stubFor(get(urlEqualTo("/pdv/userinfo/USERID_OK_1"))
                .withHeader("x-api-key", equalTo("x_api_key"))
                .willReturn(okJson("{\"pii\":\"fiscalCode\"}")));

        Mono<UserIdPDV> result = userFiscalCodeRestClient.retrieveUserId("USERID_OK_1");
        assertNotNull(result);
    }

    @Test
    void retrieveUserInfoNotFound() {
        // Stub per USERID_NOTFOUND_1
        stubFor(get(urlEqualTo("/pdv/USERID_NOTFOUND_1"))
                .willReturn(aResponse().withStatus(404)));
        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo("USERID_NOTFOUND_1").block();
        assertNull(result);
    }

    @Test
    void retrieveUserInfoInternalServerError() {
        stubFor(get(urlEqualTo("/pdv/USERID_INTERNALSERVERERROR_1"))
                .willReturn(aResponse().withStatus(500)));

        UserInfoPDV result = userFiscalCodeRestClient
                .retrieveUserInfo("USERID_INTERNALSERVERERROR_1")
                .block();

        assertNull(result); // non viene lanciata eccezione
    }

    @Test
    void retrieveUserInfoBadRequest() {
        // Stub per USERID_BADREQUEST_1
        stubFor(get(urlEqualTo("/pdv/USERID_BADREQUEST_1"))
                .willReturn(aResponse().withStatus(400)));
        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo("USERID_BADREQUEST_1").block();
        assertNull(result);
    }

    @Test
    void retrieveUserInfoTooManyRequest() {
        stubFor(get(urlEqualTo("/pdv/USERID_TOOMANYREQUEST_1"))
                .willReturn(aResponse().withStatus(429)));

        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo("USERID_TOOMANYREQUEST_1").block();
        assertNull(result);
    }

    @Test
    void retrieveUserInfoHttpForbidden() {
        stubFor(get(urlEqualTo("/pdv/USERID_FORBIDDEN_1"))
                .willReturn(aResponse().withStatus(403)));

        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo("USERID_FORBIDDEN_1").block();
        assertNull(result);
    }
}