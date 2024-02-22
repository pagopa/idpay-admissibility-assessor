package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
@ExtendWith(MockitoExtension.class)
class CriteriaCodesConfigurationTest {
    @Mock
    private CriteriaCodesConfiguration criteriaCodesConfiguration;

    @Test
    void givenGetCriteriaCongigsThenReturnMapOfCriteriaCodeConfig(){
        //Given
        Map<String, CriteriaCodeConfig> expected = Map.of(
                "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "Istituto Nazionale Previdenza Sociale", "isee"),
                "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "Agenzia per l'Italia Digitale", "birthDate"),
                "RESIDENCE", new CriteriaCodeConfig("RESIDENCE", "AGID","Agenzia per l'Italia Digitale",  "residence"),
                "FAMILY", new CriteriaCodeConfig("FAMILY", "INPS","Istituto Nazionale Previdenza Sociale",  "family")
        );

        Mockito.when(criteriaCodesConfiguration.getCriteriaCodeConfigs()).thenReturn(expected);
        //When
        Map<String, CriteriaCodeConfig> result = criteriaCodesConfiguration.getCriteriaCodeConfigs();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expected, result);
        Assertions.assertEquals(4,result.size());
    }
}
