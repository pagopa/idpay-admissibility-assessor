package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorAnd;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorOr;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImplTest;
import lombok.Data;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.command.CommandFactory;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExtraFilter2DroolsTransformerFacadeImplTest {
    public static final String STRINGVALUE = "STRINGVALUE";
    private static final BigDecimal BIGDECIMALVALUE = BigDecimal.valueOf(123);
    private static final LocalDate LOCALDATEOBJECT = LocalDate.of(2022, 7, 15);
    private static final LocalTime LOCALTIMEOBJECT = LocalTime.of(5, 1, 59, 999000000);
    private static final LocalDateTime LOCALDATETIMEOBJECT = LocalDateTime.of(LOCALDATEOBJECT, LOCALTIMEOBJECT);
    private static final ZoneOffset ZONEOFFSETOBJECT = ZoneOffset.ofHours(0);
    private static final ZoneId ZONEIDOBJECT = ZoneId.of(ZONEOFFSETOBJECT.toString());
    private static final ZonedDateTime ZONEDDATETIMEOBJECT = ZonedDateTime.of(LOCALDATEOBJECT, LOCALTIMEOBJECT, ZONEIDOBJECT);
    private static final OffsetDateTime OFFSETDATETIMEOBJECT = OffsetDateTime.of(LOCALDATEOBJECT, LOCALTIMEOBJECT, ZONEOFFSETOBJECT);

    public static ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade =
            new ExtraFilter2DroolsTransformerFacadeImpl();

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
    }

    @Data
    public static class ExtraFilterTestModelSample {
        private String stringObject;
        private Number numberObject;
        private DayOfWeek enumObject;
        private Collection<String> collectionObject;
        private LocalDate localDateObject;
        private LocalTime localTimeObject;
        private LocalDateTime localDateTimeObject;
        private ZoneOffset zoneOffsetObject;
        private ZoneId zoneIdObject;
        private ZonedDateTime zonedDateTimeObject;
        private OffsetDateTime offsetDateTimeObject;
        private Boolean booleanObject;
    }

    @Test
    public void testSuccessful() {
        ExtraFilter extraFilter = new AggregatorAnd(Arrays.asList(
                new Filter("stringObject", FilterOperator.EQ, null),
                new Filter("stringObject", FilterOperator.GE, STRINGVALUE),
                new AggregatorOr(List.of(
                        new AggregatorAnd(Arrays.asList(
                                new Filter("numberObject", FilterOperator.INSTANCE_OF, BigDecimal.class.getName()),
                                new Filter("numberObject", FilterOperator.LT, BIGDECIMALVALUE.toString()),
                                new Filter("numberObject", FilterOperator.EQ, null)
                        )),
                        new AggregatorAnd(List.of(
                                new Filter("enumObject", FilterOperator.IN, "(" + DayOfWeek.MONDAY + "," + DayOfWeek.FRIDAY + ")"),
                                new Filter("enumObject", FilterOperator.EQ, null)
                        ))
                )),
                new AggregatorOr(List.of(

                        new AggregatorAnd(List.of(
                                new Filter("collectionObject", FilterOperator.INSTANCE_OF, HashSet.class.getName()),
                                new Filter("collectionObject", FilterOperator.EQ, BIGDECIMALVALUE.add(BigDecimal.ONE).toString())
                        )),
                        new AggregatorAnd(List.of(
                                new Filter("collectionObject", FilterOperator.INSTANCE_OF, ArrayList.class.getName()),
                                new Filter("collectionObject", FilterOperator.EQ, null)
                        ))
                )),

                new Filter("localDateObject", FilterOperator.GT, LOCALDATEOBJECT.format(DateTimeFormatter.ISO_LOCAL_DATE)),
                new Filter("localDateObject", FilterOperator.EQ, null),
                new NotOperation(
                        new Filter("localDateObject.dayOfWeek", FilterOperator.NOT_EQ, DayOfWeek.MONDAY.toString())
                ),
                new Filter("localTimeObject", FilterOperator.LE, LOCALTIMEOBJECT.format(DateTimeFormatter.ISO_LOCAL_TIME)),
                new Filter("localTimeObject", FilterOperator.EQ, null),

                new Filter("localDateTimeObject", FilterOperator.NOT_EQ, LOCALDATETIMEOBJECT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                new Filter("localDateTimeObject", FilterOperator.EQ, null),

                new Filter("localDateTimeObject", FilterOperator.EQ, LOCALDATETIMEOBJECT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                new Filter("localDateTimeObject", FilterOperator.EQ, null),

                new Filter("zoneOffsetObject", FilterOperator.EQ, ZONEOFFSETOBJECT.toString()),
                new Filter("zoneOffsetObject", FilterOperator.EQ, null),

                new Filter("zoneIdObject", FilterOperator.EQ, ZONEIDOBJECT.toString()),
                new Filter("zoneIdObject", FilterOperator.EQ, null),

                new Filter("zonedDateTimeObject", FilterOperator.EQ, ZONEDDATETIMEOBJECT.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)),
                new Filter("zonedDateTimeObject", FilterOperator.EQ, null),

                new Filter("offsetDateTimeObject", FilterOperator.EQ, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.EQ, null),

                new Filter("booleanObject", FilterOperator.EQ, "true"),
                new Filter("booleanObject", FilterOperator.EQ, null)

        ));
        String result = extraFilter2DroolsTransformerFacade.apply(extraFilter, ExtraFilterTestModelSample.class, null);

        String expected = "(" +
                "stringObject == null && " +
                "stringObject >= \"STRINGVALUE\" && " +
                "(" +
                "(numberObject instanceof java.math.BigDecimal && numberObject < new java.math.BigDecimal(\"123\") && numberObject == null) || " +
                "(enumObject in (java.time.DayOfWeek.valueOf(\"MONDAY\"),java.time.DayOfWeek.valueOf(\"FRIDAY\")) && enumObject == null)" +
                ") && " +
                "(" +
                "(collectionObject instanceof java.util.HashSet && collectionObject contains \"124\") || " +
                "(collectionObject instanceof java.util.ArrayList && collectionObject == null)" +
                ") && " +
                "localDateObject > java.time.LocalDate.of(2022,7,15) && localDateObject == null && " +
                "!(" +
                "localDateObject.dayOfWeek != java.time.DayOfWeek.valueOf(\"MONDAY\")" +
                ") && " +
                "localTimeObject <= java.time.LocalTime.of(5,1,59,999000000) && localTimeObject == null && " +
                "localDateTimeObject != java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000)) && localDateTimeObject == null && " +
                "localDateTimeObject == java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000)) && localDateTimeObject == null && " +
                "zoneOffsetObject == java.time.ZoneOffset.of(\"Z\") && zoneOffsetObject == null && zoneIdObject == java.time.ZoneOffset.of(\"Z\") && " +
                "zoneIdObject == null && zonedDateTimeObject == java.time.ZonedDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneId.of(\"Z\")) && " +
                "zonedDateTimeObject == null && offsetDateTimeObject.isEqual(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "offsetDateTimeObject.isEqual(null) && " +
                "booleanObject == true && booleanObject == null)";
        Assertions.assertEquals(expected, result);

        DroolsRule rule = new DroolsRule();
        rule.setId("ID");
        rule.setName("NAME");
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(),
                "$sample: %s(%s)".formatted(ExtraFilterTestModelSample.class.getName(), result),
                ""
        ));
        new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule)).block();
    }

    @Test
    public void testErrors() {
        try {
            extraFilter2DroolsTransformerFacade.apply(new Filter("DUMMY", FilterOperator.EQ, ""), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field DUMMY on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("No such method error tested");
        }

        String expectedClassLogginError = "it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.ScalarOpValueBuilder";
        try {
            ((Logger) LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.OFF);
            extraFilter2DroolsTransformerFacade.apply(new Filter("isee", FilterOperator.EQ, "abc"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Unsupported value provided for the field isee: it is supposed to be a %s", BigDecimal.class), e.getMessage());
            System.out.println("Unsupported value tested");
        } finally {
            ((Logger) LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.INFO);
        }

        try {
            extraFilter2DroolsTransformerFacade.apply(new Filter("isee()", FilterOperator.EQ, "123"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field isee() on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("Method not found tested");
        }

        try {
            extraFilter2DroolsTransformerFacade.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "123"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("The Class defined as cast of the field isee of %s is not assignable to field type %s", OnboardingDroolsDTO.class, BigDecimal.class.getName()), e.getMessage());
            System.out.println("Class cast exception during implicit cast tested");
        }

        try {
            extraFilter2DroolsTransformerFacade.apply(new Filter("isee", FilterOperator.INSTANCE_OF, String.class.getName()), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Unsupported Class provided for the field isee", e.getMessage());
            System.out.println("Explicit Class cast exception tested");
        }

        try {
            extraFilter2DroolsTransformerFacade.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "DUMMY"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("The Class defined as cast of the field isee of " + OnboardingDroolsDTO.class + " is not assignable to field type java.math.BigDecimal", e.getMessage());
            System.out.println("Implicit Class cast exception tested");
        }
    }

    @Test
    public void testImplicitCast() {
        String result = extraFilter2DroolsTransformerFacade.apply(new Filter("(" + LocalDateTime.class.getName() + ")criteriaConsensusTimestamp.(java.time.Month)month", FilterOperator.EQ, "JANUARY"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("((criteriaConsensusTimestamp instanceof " + LocalDateTime.class.getName() + ") && (criteriaConsensusTimestamp.month instanceof java.time.Month) && criteriaConsensusTimestamp.month == java.time.Month.valueOf(\"JANUARY\"))", result);
    }

    @Test
    public void testCollection() {
        OnboardingDroolsDTO onboarding = new OnboardingDroolsDTO();
        onboarding.setInitiativeId("id");
        onboarding.setSelfDeclarationList(new HashMap<>());
        onboarding.getSelfDeclarationList().put("KEY1", true);
        onboarding.getSelfDeclarationList().put("KEY2", false);
        onboarding.getSelfDeclarationList().put("KEY3", null);

        DroolsRule rule = new DroolsRule();
        rule.setId(onboarding.getInitiativeId());
        rule.setName("CollectionTest");
        String ruleConsequence = "$onboarding.getOnboardingRejectionReasons().add(\"OK\");";

        System.out.println("Testing Collection EQ value matching");
        String result = extraFilter2DroolsTransformerFacade.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY1"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY1\"", result);
        String ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, true);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ value not matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY4"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY4\"", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, false);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ collection matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2,KEY3)"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY2\",\"KEY1\",\"KEY3\"))", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, true);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ collection not matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1)"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY1\"))", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, false);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        result = extraFilter2DroolsTransformerFacade.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2,KEY4)"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY2\",\"KEY1\",\"KEY4\"))", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, false);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());
    }

    private void checkCollectionResult(OnboardingDroolsDTO onboardingDroolsDTO, DroolsRule rule, boolean success) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNOREDRULE");
        ignoredRule.setRule(applyRuleTemplate(ignoredRule.getId(), ignoredRule.getName(), "eval(true)", "throw new RuntimeException(\"This should not occur\");"));

        KieContainer container = new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule, ignoredRule)).block();
        Assertions.assertNotNull(container);

        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(onboardingDroolsDTO),
                new AgendaGroupSetFocusCommand(onboardingDroolsDTO.getInitiativeId())
        );
        container.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));

        Assertions.assertEquals(success, onboardingDroolsDTO.getOnboardingRejectionReasons().contains("OK"), "Unexpected result applying rule%n%s%non object:%n%s".formatted(rule.getRule(), onboardingDroolsDTO));
    }

    public static String applyRuleTemplate(String agendaGroup, String ruleName, String ruleCondition, String ruleConsequence) {
        return """
                package dummy;
                                
                rule "%s"
                agenda-group "%s"
                when %s
                then %s
                end
                """.formatted(
                ruleName,
                agendaGroup,
                ruleCondition,
                ruleConsequence);
    }
}
