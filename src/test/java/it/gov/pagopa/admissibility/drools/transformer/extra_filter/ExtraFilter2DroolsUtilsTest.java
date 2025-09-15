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
            "budgetReserved"
    ));

    static final Map<String, Class<?>> expectedFields2Class = Map.ofEntries(
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
            Map.entry("verifyIsee", Boolean.class),
            Map.entry("underThreshold", Boolean.class)
    );

    public static final Set<String> expectedFields = expectedFields2Class.keySet();

    @Test
    void testBuildExtraFilterFields() {
        System.out.println("Testing null parameters (only nullable)");
        List<ExtraFilterField> result = ExtraFilter2DroolsUtils.buildExtraFilterFields(OnboardingDTO.class, null, ignoredPaths);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedFields, result.stream().map(ExtraFilterField::getField).collect(Collectors.toSet()));

        System.out.println("Testing invalid subclass");
        Map<Class<?>, List<Class<?>>> classMapping = Collections.singletonMap(BigDecimal.class, Collections.singletonList(OnboardingDTO.class));
        try {
            ExtraFilter2DroolsUtils.buildExtraFilterFields(OnboardingDTO.class, classMapping, ignoredPaths);
            Assertions.fail("Subclass check failed");
        } catch (IllegalArgumentException e) {
            //Nothing to do!
        }

        System.out.println("Testing ignore path");
        Set<String> ignoredPaths2 = new HashSet<>(ignoredPaths);
        ignoredPaths2.add("isee");
        result = ExtraFilter2DroolsUtils.buildExtraFilterFields(OnboardingDTO.class, null, ignoredPaths2);
        Set<String> expectedField2 = new HashSet<>(expectedFields);
        expectedField2.remove("isee");
        Assertions.assertEquals(expectedField2, result.stream().map(ExtraFilterField::getField).collect(Collectors.toSet()));

        System.out.println("Test complete");
        result = ExtraFilter2DroolsUtils.buildExtraFilterFields(OnboardingDTO.class, null, ignoredPaths);
        checkOnboardingDTOFields(result);
    }

    private void checkOnboardingDTOFields(List<ExtraFilterField> result) {
        Assertions.assertEquals(expectedFields, result.stream().map(ExtraFilterField::getField).collect(Collectors.toSet()));
        for (ExtraFilterField field : result) {
            if (field.getField().startsWith("(")) {
                Assertions.assertNotNull(field.getCastPath());
                Assertions.assertEquals("(" + field.getCastPath().getName() + ")paymentMethod." + field.getName(), field.getField());
            } else if (field.getName().equals("paymentMethod")) {
                Assertions.assertTrue(field.isToCast());
            }
        }

        Map<String, Object> context = new HashMap<>();
        result.sort(Comparator.comparing(ExtraFilterField::getField));
        result.forEach(f -> {
            Class<?> determinedType = Filter2DroolsTransformer.determineFieldType(f.getField(), OnboardingDTO.class, context);
            Assertions.assertEquals(expectedFields2Class.get(f.getField()), determinedType);
        });
    }
}

