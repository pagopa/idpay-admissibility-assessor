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
                "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "isee"),
                "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "birthDate"),
                "RESIDENZA", new CriteriaCodeConfig("RESIDENZA", "AGID", "residenza")
        );

        Assertions.assertEquals(expected, result);
    }
}
