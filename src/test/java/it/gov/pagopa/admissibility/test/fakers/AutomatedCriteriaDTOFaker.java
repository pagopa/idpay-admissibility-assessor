package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public final class AutomatedCriteriaDTOFaker {
    private AutomatedCriteriaDTOFaker(){}

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    //region mock automated criteria build
    private static final List<FilterOperator> admissibleFilterOperator = List.of(
            FilterOperator.EQ,
            FilterOperator.NOT_EQ,
            FilterOperator.LT,
            FilterOperator.LE,
            FilterOperator.GT,
            FilterOperator.GE,
            FilterOperator.IN
    );

    /** It will return an example of {@link AutomatedCriteriaDTOFaker}. If not provided a criteriaCode, it will be chosen randomly. Providing a bias, it will return a pseudo-casual object */
    public static AutomatedCriteriaDTO mockInstance(String criteriaCode, Integer bias) {
        Random random = getRandom(bias);

        AutomatedCriteriaDTO out = new AutomatedCriteriaDTO();
        CriteriaCodeConfig criteriaCodeConfig = criteriaCode != null ? CriteriaCodeConfigFaker.mockedCriteriaCodes.stream().filter(c -> c.getCode().equals(criteriaCode)).findFirst().orElse(null) : CriteriaCodeConfigFaker.mockInstance(bias);
        Objects.requireNonNull(criteriaCodeConfig, "Provided a not valid criteria code: %s".formatted(criteriaCode));

        out.setAuthority(criteriaCodeConfig.getAuthority());
        out.setCode(criteriaCodeConfig.getCode());
        out.setField(mockCriteriaCodeField(criteriaCode, bias));

        out.setOperator(admissibleFilterOperator.get(random.nextInt() % admissibleFilterOperator.size()));

        out.setValue(mockAutomatedCriteriaValue(out.getCode(), out.getField(), bias));

        return out;
    }
    //endregion

    //region mock automated criteria value build
    private static final Map<String, Map<String, String>> criteriaCode2FieldRegexify = Map.ofEntries(
            ImmutablePair.of(
                    CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE,
                    Map.ofEntries(ImmutablePair.of(null, "[0-9]{3}"))),
            ImmutablePair.of(
                    CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE,
                    Map.of("anno", "[0-9]{4}", "eta", "[0-9]{2}")),
            ImmutablePair.of(
                    CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENZA,
                    Map.of("citta", "[A-Z]{6}", "cap", "[0-9]{5}", "nazione", "[A-Z]{4}", "regione", "[A-Z]{5}"))
    );

    /** Given a criteriaCode and it's field, it will return a random allowable value. If bias is not null, the value will be pseudo-casual */
    public static String mockAutomatedCriteriaValue(String criteriaCode, String field, Integer bias) {
        String regexifyField = Objects.requireNonNull(
                Objects.requireNonNull(criteriaCode2FieldRegexify.get(criteriaCode), "Provided a not valid criteria code: %s".formatted(criteriaCode))
                .get(field), "Provided a not valid field (%s) for criteria code %s".formatted(field, criteriaCode));
        return new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias))).regexify(regexifyField);
    }
    //endregion

    //region mock criteria code's field selection
    private static final Map<String, List<String>> criteriaCode2AllowedNestedFields = Map.ofEntries(
            buildCriteriaCode2AllowedNestedFieldsEntry(CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE),
            buildCriteriaCode2AllowedNestedFieldsEntry(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENZA)
    );

    private static Map.Entry<String, List<String>> buildCriteriaCode2AllowedNestedFieldsEntry(String criteriaCode) {
        return new ImmutablePair<>(criteriaCode,
                List.copyOf(
                        Objects.requireNonNull(criteriaCode2FieldRegexify.get(criteriaCode), "Provided a criteria code not configured in criteriaCode2FieldRegexify: %s".formatted(criteriaCode))
                                .keySet()));
    }

    /** Given a criteriaCode, it will return a random allowable field. If bias is not null, the value will be pseudo-casual */
    public static String mockCriteriaCodeField(String criteriaCode, Integer bias) {
        List<String> allowedFields = criteriaCode2AllowedNestedFields.get(criteriaCode);
        return switch (criteriaCode) {
            case CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE -> null;
            case CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE, CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENZA -> allowedFields.get(getRandom(bias).nextInt() % allowedFields.size());
            default -> throw new IllegalArgumentException("Criteria code %s not configured in AutomatedCriteriaDTOFaker.mockCriteriaCodeField".formatted(criteriaCode));
        };
    }
    //endregion
}
