package it.gov.pagopa.admissibility.rest;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings("squid:S3577") // suppressing class name not match alert
@TestPropertySource(locations = {
        "classpath:/secrets/pdndConfig.properties"
})
@ContextConfiguration(inheritInitializers = false)
class PdndCreateTokenRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private PdndCreateTokenRestClient pdndCreateTokenRestClient;

    @Value("${app.pdnd.token-pdnd}")
    private String apiKeyClientAssertion;

    @Value("${app.pdnd.properties.clientId}")
    private String apiKeyClientId;

    @Test
    void getToken(){
        ApiKeysPDND apiKeysPDND_1 = ApiKeysPDND.builder()
                .apiKeyClientId(apiKeyClientId)
                .apiKeyClientAssertion(apiKeyClientAssertion)
                .build();

        ClientCredentialsResponseDTO result = pdndCreateTokenRestClient.createToken(apiKeysPDND_1).block();

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

    }
}