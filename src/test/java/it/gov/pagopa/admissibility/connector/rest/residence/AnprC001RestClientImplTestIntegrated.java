package it.gov.pagopa.admissibility.connector.rest.residence;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service.AnprC001RestClient;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(locations = {"classpath:secrets/anpr.properties"})
@ContextConfiguration(inheritInitializers = false)
class AnprC001RestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private CustomSequenceGeneratorRepository sequenceGeneratorRepository;

    @Autowired
    private AnprC001RestClient anprC001RestClient;

    @Test
    void getResidenceAssessment(){
        // Given
        String accessToken = "VALID_ACCESS_TOKEN_1.eyJhdWQiOiJodHRwczovL21vZGlwYS12YWwuYW5wci5pbnRlcm5vLml0L2dvdndheS9yZXN0L2luL01pbkludGVybm9Qb3J0YUFOUFIvQzAwMS1zZXJ2aXppb05vdGlmaWNhL3YxIiwic3ViIjoiNWRmOWYyMTgtZGYzNC00ZjA1LWE5YzQtZTRhNzRiMmE4ZTNmIiwibmJmIjoxNjk1MTMxNDI0LCJkaWdlc3QiOnsiYWxnIjoiU0hBMjU2IiwidmFsdWUiOiI1QTUyMDhBRUI4NDVGRjFGNkY1RjUzQjVFNjg4MUU4NTJBNEY5QTUzQkNEMTE0MUE5MkM1OUNDRDlERkI1QjhBIn0sInB1cnBvc2VJZCI6IjhhZjRjNzk1LTgwNjYtNDQwNy1hMGIzLWM5NWMyZGFlZmQ2YyIsImlzcyI6InVhdC5pbnRlcm9wLnBhZ29wYS5pdCIsImV4cCI6MTY5NTEzMjAyNCwiaWF0IjoxNjk1MTMxNDI0LCJjbGllbnRfaWQiOiI1ZGY5ZjIxOC1kZjM0LTRmMDUtYTljNC1lNGE3NGIyYThlM2YiLCJqdGkiOiI2Y2YxYjQ0YS0wNDA3LTQ4MDItYWZiZi1mNzhkNjQyODYyZDcifQ.iz31xKOKuJ5j3-CQ39KfZxCndNHLUBlOjZLffiJX3Gn_bwJ0QfPbHArXBB_7OuvtzNinRk8K24tMSstY0KZUVFjiq-WuYkdnjvgFqUXjnpJsGQqJtZqDOdkUY3f1SjPRRbuZzBIMrkC25BWvUHua2BT4KZS4PtFrSK_N7DCzJQ0JdKK5VOWkqcM4fcab22-9NJ6laCDSHT2g_Kyn_tLRy9jQwM1B_cLW47qEas5odeQ0fGVjanB0ONSjhl2gQRcHJKbA-rm4s7WdMGAagy6T20R_vRe9Whhgh7qlPC04CaMhiPbJTzqEFu7QucBz0uT7drUnPSZSQglpcWT25lk6QA";
        String fiscalCode = "FISCAL_CODE";
        int sequenceVal = 0;

        AgidJwtTokenPayload agidTokenPayload = AgidJwtTokenPayload.builder()
                .iss("5df9f218-df34-4f05-a9c4-e4a74b2a8e3f")
                .sub("5df9f218-df34-4f05-a9c4-e4a74b2a8e3f")
                .build();

        sequenceGeneratorRepository.save(new CustomSequenceGenerator(OnboardingConstants.ANPR_E002_INVOKE, sequenceVal)).block();

        // When
        RispostaE002OKDTO result = anprC001RestClient.invoke(accessToken, fiscalCode,agidTokenPayload).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getIdOperazioneANPR());
        System.out.println(result);
    }
}