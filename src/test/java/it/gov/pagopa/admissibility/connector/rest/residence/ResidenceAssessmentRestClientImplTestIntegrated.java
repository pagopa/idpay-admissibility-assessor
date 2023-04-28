package it.gov.pagopa.admissibility.connector.rest.residence;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.rest.anpr.residence.ResidenceAssessmentRestClient;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(properties = {
        "app.anpr.c020-residenceAssessment.base-url=https://api-io.dev.cstar.pagopa.it/mock-ex-serv-anpr"
})
@ContextConfiguration(inheritInitializers = false)
class ResidenceAssessmentRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private ResidenceAssessmentRestClient residenceAssessmentRestClient;

    @Test
    void getResidenceAssessment(){
        // Given
        String accessToken = "VALID_ACCESS_TOKEN_1";
        String fiscalCode = "FISCAL_CODE";

        AgidJwtTokenPayload agidTokenPayload = AgidJwtTokenPayload.builder()
                .iss("ISS")
                .sub("SUB")
                .aud("AUD").build();

        // When
        RispostaE002OKDTO result = residenceAssessmentRestClient.getResidenceAssessment(accessToken, fiscalCode,agidTokenPayload).block();

        // Then
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}