package it.gov.pagopa.admissibility.test.fakers;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

public final class CriteriaCodeConfigFaker {
    private CriteriaCodeConfigFaker(){}

    public static final String CRITERIA_CODE_ISEE = "ISEE";
    public static final String CRITERIA_CODE_ISEE_AUTH = "INPS";
    public static final String CRITERIA_CODE_ISEE_AUTH_LABEL = "Istituto Nazionale Previdenza Sociale";

    public static final String CRITERIA_CODE_BIRTHDATE = "BIRTHDATE";
    public static final String CRITERIA_CODE_BIRTHDATE_AUTH = "AGID";
    public static final String CRITERIA_CODE_BIRTHDATE_AUTH_LABEL = "Agenzia per l'Italia Digitale";

    public static final String CRITERIA_CODE_RESIDENCE = "RESIDENCE";
    public static final String CRITERIA_CODE_RESIDENCE_AUTH = "AGID";
    public static final String CRITERIA_CODE_RESIDENCE_AUTH_LABEL = "Agenzia per l'Italia Digitale";

    public static final String CRITERIA_CODE_FAMILY = "FAMILY";
    public static final String CRITERIA_CODE_FAMILY_AUTH = "INPS";
    public static final String CRITERIA_CODE_FAMILY_AUTH_LABEL = "Istituto Nazionale Previdenza Sociale";

    /** mocked criteria codes */
    public static List<CriteriaCodeConfig> mockedCriteriaCodes = List.of(
            new CriteriaCodeConfig(CRITERIA_CODE_ISEE, CRITERIA_CODE_ISEE_AUTH, CRITERIA_CODE_ISEE_AUTH_LABEL, "isee"),
            new CriteriaCodeConfig(CRITERIA_CODE_BIRTHDATE, CRITERIA_CODE_BIRTHDATE_AUTH, CRITERIA_CODE_BIRTHDATE_AUTH_LABEL, "birthDate"),
            new CriteriaCodeConfig(CRITERIA_CODE_RESIDENCE, CRITERIA_CODE_RESIDENCE_AUTH, CRITERIA_CODE_RESIDENCE_AUTH_LABEL, "residence"),
            new CriteriaCodeConfig(CRITERIA_CODE_FAMILY, CRITERIA_CODE_FAMILY_AUTH, CRITERIA_CODE_FAMILY_AUTH_LABEL, "residence")
    );

    public static void configCriteriaCodeServiceMock(CriteriaCodeService criteriaCodeServiceMock){
        CriteriaCodeConfigFaker.mockedCriteriaCodes.forEach(c -> Mockito.lenient().when(criteriaCodeServiceMock.getCriteriaCodeConfig(c.getCode())).thenReturn(c));
    }

    private static final Random random = new Random();

    /** It will return an example of {@link CriteriaCodeConfig}. Providing a bias, it will return a pseudo-casual object */
    public static CriteriaCodeConfig mockInstance(Integer bias){
        int index = (bias == null ? random.nextInt() : bias) % mockedCriteriaCodes.size();

        return mockedCriteriaCodes.get(index);
    }
}
