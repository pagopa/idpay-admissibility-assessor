package it.gov.pagopa.admissibility.rest;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/secrets/pdndConfig.properties"
})
@ContextConfiguration(inheritInitializers = false)
class PdndCreateTokenRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private PdndCreateTokenRestClient pdndCreateTokenRestClient;

    @Value("${app.pdnd.token-pdnd}")
    private String pdndToken;

    @Test
    void getToken(){

        ClientCredentialsResponseDTO result = pdndCreateTokenRestClient.createToken(pdndToken).block();

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

    }
}