package it.gov.pagopa.admissibility.rest;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class PdndCreateTokenRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private PdndCreateTokenRestClient pdndCreateTokenRestClient;

    @Test
    void createTokenOk() {
        String pdndToken = "PDND_TOKEN_1"; //TODO add check in the request stub

        ClientCredentialsResponseDTO response = pdndCreateTokenRestClient.createToken(pdndToken).block();

        Assertions.assertNotNull(response);
        Assertions.assertEquals("ACCESS_TOKEN_OK",response.getAccessToken());
    }
}