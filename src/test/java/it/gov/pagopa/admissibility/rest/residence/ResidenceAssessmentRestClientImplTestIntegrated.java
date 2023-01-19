package it.gov.pagopa.admissibility.rest.residence;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.rest.anpr.residence.ResidenceAssessmentRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

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

        // When
        RispostaE002OKDTO result = residenceAssessmentRestClient.getResidenceAssessment(accessToken, fiscalCode).block();

        // Then
        Assertions.assertNotNull(result);
        System.out.println(result);
    }
}