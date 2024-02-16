package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.service.CriteriaCodeServiceImpl=WARN",
})
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"})
class CriteriaCodeServiceIntegrationTestDeprecated extends BaseIntegrationTest {
    @Autowired
    private CriteriaCodesConfiguration criteriaCodesConfiguration;

    @Test
    void test(){
        Map<String, CriteriaCodeConfig> result = criteriaCodesConfiguration.getCriteriaCodeConfigs();

        Map<String, CriteriaCodeConfig> expected = Map.of(
                "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "Istituto Nazionale Previdenza Sociale", "isee"),
                "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "Agenzia per l'Italia Digitale", "birthDate"),
                "RESIDENCE", new CriteriaCodeConfig("RESIDENCE", "AGID","Agenzia per l'Italia Digitale",  "residence"),
                "FAMILY", new CriteriaCodeConfig("FAMILY", "INPS","Istituto Nazionale Previdenza Sociale",  "family")
        );

        Assertions.assertEquals(expected, result);
    }
}
