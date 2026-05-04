package it.gov.pagopa.common.reactive.pdv.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(classes = {
        UserFiscalCodeRestClientImpl.class,
        WebClientConfig.class,
        ObjectMapper.class
})
class UserIdRestClientImplTest {


    static WireMockServer wireMockServer = new WireMockServer(0);

    @BeforeAll
    static void startServer() {
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.pdv.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/pdv");
        registry.add("app.pdv.retry.delay-millis", () -> "100");
        registry.add("app.pdv.retry.max-attempts", () -> "1");
        registry.add("app.pdv.headers.x-api-key", () -> "x_api_key");
    }

    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;

    @BeforeEach
    void setupMocks() {
        wireMockServer.resetAll();
    }

    @Test
    void retrieveUserInfoOk() {
        stubFor(get(urlEqualTo("/pdv/CF_OK"))
                .willReturn(okJson("{\"token\":\"USERID_OK\"}")));

        UserIdPDV result = userFiscalCodeRestClient.retrieveUserId("CF_OK").block();

        Assertions.assertNull(result);
    }

    @Test
    void retrieveUserInfoNotFound() {
        stubFor(get(urlEqualTo("/pdv/tokens/USER_NOT_FOUND_1/pii"))
                .willReturn(aResponse().withStatus(404)));

        UserInfoPDV result =
                userFiscalCodeRestClient.retrieveUserInfo("USER_NOT_FOUND_1").block();

        Assertions.assertNull(result);
    }

    @Test
    void retrieveUserInfoInternalServerError() {
        stubFor(get(urlEqualTo("/pdv/tokens/USERID_INTERNALSERVERERROR_1/pii"))
                .willReturn(aResponse().withStatus(500)));

        Assertions.assertThrows(WebClientResponseException.InternalServerError.class,
                () -> userFiscalCodeRestClient.retrieveUserInfo("USERID_INTERNALSERVERERROR_1").block());
    }

    @Test
    void retrieveUserInfoBadRequest() {
        stubFor(get(urlEqualTo("/pdv/USERID_BADREQUEST_1"))
                .willReturn(aResponse().withStatus(400)));

        UserInfoPDV result =
                userFiscalCodeRestClient.retrieveUserInfo("USERID_BADREQUEST_1").block();

        Assertions.assertNull(result);
    }
}