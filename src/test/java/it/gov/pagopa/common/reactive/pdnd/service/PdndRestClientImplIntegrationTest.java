package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.common.pdnd.generated.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.reactive.pdnd.service.PdndRestClientImpl=WARN",
        "app.pdnd.base-url=http://localhost:${wiremock.server.port}/pdnd",

        "app.web-client.connect.timeout.millis=10000",
        "app.web-client.response.timeout=60000",
        "app.web-client.read.handler.timeout=60000",
        "app.web-client.write.handler.timeout=60000"
})
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stub/mappings")
@SpringBootTest(classes = {PdndRestClientImpl.class, WebClientConfig.class})
class PdndRestClientImplIntegrationTest {

    @Autowired
    private PdndRestClient pdndRestClient;

    @Test
    void createTokenOk() {
        String clientId="CLIENTID";
        String clientAssertion="CLIENTASSERTION";

        ClientCredentialsResponseDTO response = pdndRestClient.createToken(clientId, clientAssertion).block();

        Assertions.assertNotNull(response);
        Assertions.assertEquals("VALID_ACCESS_TOKEN_1",response.getAccessToken());
    }
}