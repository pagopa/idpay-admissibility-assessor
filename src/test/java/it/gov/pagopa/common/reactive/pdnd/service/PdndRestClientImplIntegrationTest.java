package it.gov.pagopa.common.reactive.pdnd.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.pdnd.generated.api.AuthApi;
import it.gov.pagopa.common.pdnd.generated.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {PdndRestClientImpl.class, WebClientConfig.class})
class PdndRestClientImplIntegrationTest {

    static WireMockServer wireMockServer = new WireMockServer(0); // porta random

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
        registry.add("app.pdnd.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/pdnd");
    }

    @Autowired
    private PdndRestClient pdndRestClient;
    @MockitoBean
    private AuthApi authApi;

    @BeforeEach
    void resetMocks() {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/pdnd/token.oauth2"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")) // se il client invia form data
                .withRequestBody(containing("client_assertion")) // almeno una parte del body
                .willReturn(okJson("{\"accessToken\":\"PDND_ACCESS_TOKEN\",\"expiresIn\":3600}")));
    }

    @Test
    void createTokenOk() {
        String clientId = "CLIENTID";
        String clientAssertion = "CLIENT.ASSERT.ION";

        ClientCredentialsResponseDTO response = pdndRestClient.createToken(clientId, clientAssertion).block();

        assertNotNull(response);
    }
}