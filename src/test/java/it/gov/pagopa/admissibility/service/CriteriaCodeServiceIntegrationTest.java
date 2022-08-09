package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class CriteriaCodeServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private CriteriaCodesConfiguration criteriaCodesConfiguration;

    @Test
    void test(){
        Map<String, CriteriaCodeConfig> result = criteriaCodesConfiguration.getCriteriaCodeConfigs();

        Map<String, CriteriaCodeConfig> expected = Map.of(
                "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "Istituto Nazionale Previdenza Sociale", "isee"),
                "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "Agenzia per l'Italia Digitale", "birthDate"),
                "RESIDENZA", new CriteriaCodeConfig("RESIDENZA", "AGID","Agenzia per l'Italia Digitale",  "residenza")
        );

        Assertions.assertEquals(expected, result);
    }
}
