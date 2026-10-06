package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilterField;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.Filter2DroolsTransformer;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

class ExtraFilter2DroolsUtilsTest {

    private static final Set<String> ignoredPaths = new HashSet<>(Arrays.asList(
            "userId",
            "initiativeId",
            "tc",
            "status",
            "pdndAccept",
            "selfDeclarationList",
            "tcAcceptTimestamp",
            "criteriaConsensusTimestamp",
            "family",
            "budgetReserved",
            "verifies"
    ));

    private static final Map<String, Class<?>> expectedFields2Class = Map.ofEntries(
            Map.entry("birthDate", BirthDate.class),
            Map.entry("birthDate.year", String.class),
            Map.entry("birthDate.age", Integer.class),

            Map.entry("isee", BigDecimal.class),

            Map.entry("residence", Residence.class),
            Map.entry("residence.postalCode", String.class),
            Map.entry("residence.cityCouncil", String.class),
            Map.entry("residence.province", String.class),
            Map.entry("residence.city", String.class),
            Map.entry("residence.nation", String.class),
            Map.entry("residence.region", String.class),

            Map.entry("serviceId", String.class),
            Map.entry("userMail", String.class),
            Map.entry("channel", String.class),
            Map.entry("name", String.class),
            Map.entry("surname", String.class)
    );

    private static final Set<String> expectedFields = expectedFields2Class.keySet();

    @Test
    void testBuildExtraFilterFields() {

        List<ExtraFilterField> result =
                ExtraFilter2DroolsUtils.buildExtraFilterFields(
                        OnboardingDTO.class,
                        null,
                        ignoredPaths
                );

        Assertions.assertNotNull(result);

        Assertions.assertEquals(
                expectedFields,
                result.stream()
                        .map(ExtraFilterField::getField)
                        .collect(Collectors.toSet())
        );

        Map<Class<?>, List<Class<?>>> invalidMapping =
                Collections.singletonMap(BigDecimal.class, List.of(OnboardingDTO.class));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ExtraFilter2DroolsUtils.buildExtraFilterFields(
                        OnboardingDTO.class,
                        invalidMapping,
                        ignoredPaths
                )
        );

        Set<String> ignoredPaths2 = new HashSet<>(ignoredPaths);
        ignoredPaths2.add("isee");

        List<ExtraFilterField> resultWithoutIsee =
                ExtraFilter2DroolsUtils.buildExtraFilterFields(
                        OnboardingDTO.class,
                        null,
                        ignoredPaths2
                );

        Set<String> expectedWithoutIsee = new HashSet<>(expectedFields);
        expectedWithoutIsee.remove("isee");

        Assertions.assertEquals(
                expectedWithoutIsee,
                resultWithoutIsee.stream()
                        .map(ExtraFilterField::getField)
                        .collect(Collectors.toSet())
        );

        checkOnboardingDTOFields(result);
    }

    private void checkOnboardingDTOFields(List<ExtraFilterField> result) {

        Assertions.assertEquals(
                expectedFields,
                result.stream()
                        .map(ExtraFilterField::getField)
                        .collect(Collectors.toSet())
        );

        Map<String, Object> context = new HashMap<>();
        result.sort(Comparator.comparing(ExtraFilterField::getField));

        for (ExtraFilterField field : result) {
            Class<?> determinedType =
                    Filter2DroolsTransformer.determineFieldType(
                            field.getField(),
                            OnboardingDTO.class,
                            context
                    );

            Assertions.assertEquals(
                    expectedFields2Class.get(field.getField()),
                    determinedType,
                    "Type mismatch for field: " + field.getField()
            );
        }
    }
}