package it.gov.pagopa.admissibility.rest.residence;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResidenceAssessmentRestClientImplTest extends BaseIntegrationTest {

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
    }

}