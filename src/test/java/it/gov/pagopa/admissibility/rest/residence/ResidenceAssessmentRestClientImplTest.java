package it.gov.pagopa.admissibility.rest.residence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.rest.anpr.residence.ResidenceAssessmentRestClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.rest.anpr.residence.ResidenceAssessmentRestClientImpl=WARN",
})
@DirtiesContext
class ResidenceAssessmentRestClientImplTest extends BaseIntegrationTest {

    @SpyBean
    private ResidenceAssessmentRestClient residenceAssessmentRestClient;

    @SpyBean
    private ObjectMapper objectMapper;

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

    @Test
    @SneakyThrows
    void objectMapperException(){
        // Given
        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);

        // When
        try {
            residenceAssessmentRestClient.getResidenceAssessment("VALID_TOKEN", "FISCAL_CODE").block();
        }catch (Exception e){
            // Then
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
    }
}