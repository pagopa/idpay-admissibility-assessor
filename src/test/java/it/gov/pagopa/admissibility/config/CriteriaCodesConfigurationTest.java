package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaCodesConfigurationTest {

    private CriteriaCodesConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CriteriaCodesConfiguration();
    }

    @Test
    void testDefaultConfigsIsEmpty() {
        assertTrue(configuration.getConfigs().isEmpty(),
                "La mappa configs dovrebbe essere vuota per default");
    }

    @Test
    void testAlignKeys_setsCodeOnEachEntry() {
        CriteriaCodeConfig config1 = new CriteriaCodeConfig(null, "AUTH1", "Authority One", "field1");
        CriteriaCodeConfig config2 = new CriteriaCodeConfig(null, "AUTH2", "Authority Two", "field2");

        configuration.getConfigs().put("ISEE", config1);
        configuration.getConfigs().put("AGE", config2);

        configuration.alignKeys();

        assertEquals("ISEE", config1.getCode(),
                "Il codice dell'entry 'ISEE' deve essere allineato alla chiave della mappa");
        assertEquals("AGE", config2.getCode(),
                "Il codice dell'entry 'AGE' deve essere allineato alla chiave della mappa");
    }

    @Test
    void testAlignKeys_overwritesExistingCode() {
        CriteriaCodeConfig config = new CriteriaCodeConfig("OLD_CODE", "AUTH", "Authority", "field");
        configuration.getConfigs().put("NEW_CODE", config);

        configuration.alignKeys();

        assertEquals("NEW_CODE", config.getCode(),
                "Il codice precedente deve essere sovrascritto dalla chiave della mappa");
    }

    @Test
    void testAlignKeys_withEmptyConfigs_doesNotThrow() {
        assertDoesNotThrow(() -> configuration.alignKeys());
    }

    @Test
    void testAlignKeys_preservesOtherFields() {
        String authority = "INPS";
        String authorityLabel = "Istituto Nazionale Previdenza Sociale";
        String onboardingField = "isee";

        CriteriaCodeConfig config = new CriteriaCodeConfig(null, authority, authorityLabel, onboardingField);
        configuration.getConfigs().put("ISEE", config);

        configuration.alignKeys();

        assertEquals(authority, config.getAuthority(),
                "Il campo 'authority' non deve essere modificato da alignKeys");
        assertEquals(authorityLabel, config.getAuthorityLabel(),
                "Il campo 'authorityLabel' non deve essere modificato da alignKeys");
        assertEquals(onboardingField, config.getOnboardingField(),
                "Il campo 'onboardingField' non deve essere modificato da alignKeys");
    }

    @Test
    void testSetAndGetConfigs() {
        CriteriaCodeConfig config = new CriteriaCodeConfig("ISEE", "INPS", "INPS Label", "isee");
        Map<String, CriteriaCodeConfig> configs = Map.of("ISEE", config);

        configuration.setConfigs(new java.util.HashMap<>(configs));

        assertEquals(1, configuration.getConfigs().size());
        assertEquals(config, configuration.getConfigs().get("ISEE"));
    }
}

