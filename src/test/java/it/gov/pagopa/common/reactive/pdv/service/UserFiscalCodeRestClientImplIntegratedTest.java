package it.gov.pagopa.common.reactive.pdv.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.reactive.pdv.dto.UserIdPDV;
import it.gov.pagopa.common.reactive.pdv.dto.UserInfoPDV;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {UserFiscalCodeRestClientImpl.class, WebClientConfig.class, ObjectMapper.class})
class UserFiscalCodeRestClientImplIntegratedTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private UserFiscalCodeRestClient userFiscalCodeRestClient;

    @Value("${app.pdv.userIdOk:a85268f9-1d62-4123-8f86-8cf630b60998}")
    private String userIdOK;

    @Value("${app.pdv.userFiscalCodeExpected:A4p9Y4QUlTtutHT}")
    private String fiscalCodeOKExpected;

    @Value("${app.pdv.userIdNotFound:02105b50-9a81-4cd2-8e17-6573ebb09195}")
    private String userIdNotFound;

    // Avvia WireMock su porta random prima di tutti i test
    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    // Stop WireMock dopo tutti i test
    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    // Aggiorna le proprietà dinamiche di Spring Boot per usare WireMock
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.pdv.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/pdv");
        registry.add("app.pdv.retry.delay-millis", () -> "100");
        registry.add("app.pdv.retry.max-attempts", () -> "1");
        registry.add("app.pdv.headers.x-api-key", () -> "x_api_key");
    }

    // Reset e setup stub WireMock prima di ogni test
    @BeforeEach
    void setupWireMock() {
        wireMockServer.resetAll();

        // Stub per retrieveUserId
        stubFor(put(urlEqualTo("/pdv"))
                .willReturn(okJson("{\"token\":\"" + userIdOK + "\"}")));

        // Stub per retrieveUserInfo
        stubFor(get(urlPathMatching("/pdv/.*"))
                .willReturn(okJson("{\"pii\":\"" + fiscalCodeOKExpected + "\"}")));
    }

    @Test
    void retrieveUserInfoOk() {
        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo(userIdOK).block();
        assertNotNull(result);
        assertEquals(fiscalCodeOKExpected, result.getPii());
    }

    @Test
    void retrieveUserInfoNotFound() {
        stubFor(get(urlEqualTo("/pdv/" + userIdNotFound))
                .willReturn(aResponse().withStatus(404)));

        UserInfoPDV result = userFiscalCodeRestClient.retrieveUserInfo(userIdNotFound).block();
        assertNotNull(result);
    }

    @Test
    void retrieveUserIdNotFound() {
        stubFor(put(urlEqualTo("/pdv"))
                .willReturn(aResponse().withStatus(404)));

        UserIdPDV result = userFiscalCodeRestClient.retrieveUserId("NON_EXISTENT").block();
        assertNull(result);
    }

    @Test
    void retrieveUserIdOk() {
        UserIdPDV result = userFiscalCodeRestClient.retrieveUserId(fiscalCodeOKExpected).block();

        Assertions.assertNull(result);
    }

}