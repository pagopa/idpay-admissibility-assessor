package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDTO;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(locations = {
        "classpath:/secrets/pdndConfig.properties"
})
@ContextConfiguration(inheritInitializers = false)
class PdndRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private PdndRestClient pdndRestClient;

    @Test
    void getToken(){
        String clientId="CLIENTID";
        String clientAssertion="CLIENTASSERTION";

        ClientCredentialsResponseDTO response = pdndRestClient.createToken(clientId, clientAssertion).block();

        Assertions.assertNotNull(response);
        TestUtils.checkNotNullFields(response);

    }
}