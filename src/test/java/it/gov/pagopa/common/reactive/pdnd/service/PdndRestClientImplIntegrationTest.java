package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.reactive.pdnd.service.PdndRestClientImpl=WARN",
})
class PdndRestClientImplIntegrationTest extends BaseIntegrationTest {

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