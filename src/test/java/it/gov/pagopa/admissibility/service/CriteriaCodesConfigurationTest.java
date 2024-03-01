package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
@ExtendWith(MockitoExtension.class)
class CriteriaCodesConfigurationTest {
    @Mock private CriteriaCodesConfiguration criteriaCodesConfigurationMock;
    private CriteriaCodeServiceImpl criteriaCodesService;

    @BeforeEach
    void setUp() {
        criteriaCodesService = new CriteriaCodeServiceImpl(criteriaCodesConfigurationMock);
    }

    @Test
    void givenGetCriteriaCongigsThenReturnMapOfCriteriaCodeConfig(){
        //Given
        Map<String, CriteriaCodeConfig> expected = Map.of(
                "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "Istituto Nazionale Previdenza Sociale", "isee"),
                "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "Agenzia per l'Italia Digitale", "birthDate"),
                "RESIDENCE", new CriteriaCodeConfig("RESIDENCE", "AGID","Agenzia per l'Italia Digitale",  "residence"),
                "FAMILY", new CriteriaCodeConfig("FAMILY", "INPS","Istituto Nazionale Previdenza Sociale",  "family")
        );

        Mockito.when(criteriaCodesConfigurationMock.getCriteriaCodeConfigs()).thenReturn(expected);

        //When
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodesService.getCriteriaCodeConfig("BIRTHDATE");

        //then
        Assertions.assertNotNull(criteriaCodeConfig);
        Assertions.assertEquals(expected.get("BIRTHDATE"),criteriaCodeConfig);
    }
}
