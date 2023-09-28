package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.common.reactive.pdnd.service.PdndRestClientImpl=WARN",
})
class PdndRestClientImplTest extends BaseIntegrationTest {

    public static final String DECRYPTED_API_KEY_CLIENT_ID = "DECRYPTED_API_KEY_CLIENT_ID";
    public static final String DECRYPTED_API_KEY_CLIENT_ASSERTION = "DECRYPTED_API_KEY_CLIENT_ASSERTION";
    @Autowired
    private PdndRestClient pdndRestClient;

    @Test
    void createTokenOk() {
        ApiKeysPDND apiKeysPDND_1 = ApiKeysPDND.builder()
                .apiKeyClientId(DECRYPTED_API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(DECRYPTED_API_KEY_CLIENT_ASSERTION)
                .build();

        ClientCredentialsResponseDTO response = pdndRestClient.createToken(apiKeysPDND_1).block();

        Assertions.assertNotNull(response);
        Assertions.assertEquals("VALID_ACCESS_TOKEN_1",response.getAccessToken());
    }
}