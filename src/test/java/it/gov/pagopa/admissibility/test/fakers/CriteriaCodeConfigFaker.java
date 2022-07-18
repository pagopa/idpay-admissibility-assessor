package it.gov.pagopa.admissibility.test.fakers;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;

import java.util.List;
import java.util.Random;

public final class CriteriaCodeConfigFaker {
    private CriteriaCodeConfigFaker(){}

    public static final String CRITERIA_CODE_ISEE = "ISEE";
    public static final String CRITERIA_CODE_ISEE_AUTH = "INPS";

    public static final String CRITERIA_CODE_BIRTHDATE = "BIRTHDATE";
    public static final String CRITERIA_CODE_BIRTHDATE_AUTH = "AGID";

    public static final String CRITERIA_CODE_RESIDENZA = "RESIDENZA";
    public static final String CRITERIA_CODE_RESIDENZA_AUTH = "AGID";

    /** mocked criteria codes */
    public static List<CriteriaCodeConfig> mockedCriteriaCodes = List.of(
            new CriteriaCodeConfig(CRITERIA_CODE_ISEE, CRITERIA_CODE_ISEE_AUTH, "isee"),
            new CriteriaCodeConfig(CRITERIA_CODE_BIRTHDATE, CRITERIA_CODE_BIRTHDATE_AUTH, "birthDate"),
            new CriteriaCodeConfig(CRITERIA_CODE_RESIDENZA, CRITERIA_CODE_RESIDENZA_AUTH, "residenza")
    );

    private static final Random random = new Random();

    /** It will return an example of {@link CriteriaCodeConfig}. Providing a bias, it will return a pseudo-casual object */
    public static CriteriaCodeConfig mockInstance(Integer bias){
        int index = (bias == null ? random.nextInt() : bias) % mockedCriteriaCodes.size();

        return mockedCriteriaCodes.get(index);
    }
}
