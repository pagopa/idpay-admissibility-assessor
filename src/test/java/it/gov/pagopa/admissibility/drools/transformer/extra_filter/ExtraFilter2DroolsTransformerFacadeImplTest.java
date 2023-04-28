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
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImplTest;
import lombok.Data;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
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
    void testSuccessful() {
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

                new Filter("localDateTimeObject", FilterOperator.BTW_OPEN, LOCALDATETIMEOBJECT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), LOCALDATETIMEOBJECT.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                new Filter("localDateTimeObject", FilterOperator.BTW_CLOSED, LOCALDATETIMEOBJECT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), LOCALDATETIMEOBJECT.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                new Filter("localDateTimeObject", FilterOperator.EQ, null),

                new Filter("zoneOffsetObject", FilterOperator.EQ, ZONEOFFSETOBJECT.toString()),
                new Filter("zoneOffsetObject", FilterOperator.EQ, null),

                new Filter("zoneIdObject", FilterOperator.EQ, ZONEIDOBJECT.toString()),
                new Filter("zoneIdObject", FilterOperator.EQ, null),

                new Filter("zonedDateTimeObject", FilterOperator.BTW_CLOSED, ZONEDDATETIMEOBJECT.format(DateTimeFormatter.ISO_ZONED_DATE_TIME), ZONEDDATETIMEOBJECT.plusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)),
                new Filter("zonedDateTimeObject", FilterOperator.EQ, null),

                new Filter("offsetDateTimeObject", FilterOperator.EQ, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.EQ, null),

                new Filter("offsetDateTimeObject", FilterOperator.NOT_EQ, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.GT, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.GE, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.LT, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.LE, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                new Filter("offsetDateTimeObject", FilterOperator.BTW_OPEN, OFFSETDATETIMEOBJECT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), OFFSETDATETIMEOBJECT.plusDays(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),

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
                "localTimeObject <= java.time.LocalTime.of(5,1,59,999000000) && " +
                "localTimeObject == null && " +
                "localDateTimeObject != java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000)) && localDateTimeObject == null && " +
                "localDateTimeObject > java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000)) && localDateTimeObject < java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,16), java.time.LocalTime.of(5,1,59,999000000)) && " +
                "localDateTimeObject >= java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000)) && localDateTimeObject <= java.time.LocalDateTime.of(java.time.LocalDate.of(2022,7,16), java.time.LocalTime.of(5,1,59,999000000)) && " +
                "localDateTimeObject == null && " +
                "zoneOffsetObject == java.time.ZoneOffset.of(\"Z\") && zoneOffsetObject == null && zoneIdObject == java.time.ZoneOffset.of(\"Z\") && " +
                "zoneIdObject == null && " +
                "(!zonedDateTimeObject.isBefore(java.time.ZonedDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneId.of(\"Z\"))) && !zonedDateTimeObject.isAfter(java.time.ZonedDateTime.of(java.time.LocalDate.of(2022,7,16), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneId.of(\"Z\")))) && " +
                "zonedDateTimeObject.isEqual(null) && " +
                "offsetDateTimeObject.isEqual(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "offsetDateTimeObject.isEqual(null) && " +
                "!offsetDateTimeObject.isEqual(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "offsetDateTimeObject.isAfter(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "!offsetDateTimeObject.isBefore(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "offsetDateTimeObject.isBefore(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "!offsetDateTimeObject.isAfter(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && " +
                "(offsetDateTimeObject.isAfter(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,15), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\"))) && offsetDateTimeObject.isBefore(java.time.OffsetDateTime.of(java.time.LocalDate.of(2022,7,16), java.time.LocalTime.of(5,1,59,999000000), java.time.ZoneOffset.of(\"Z\")))) && " +
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
    void testErrors() {
        Filter filter = new Filter("DUMMY", FilterOperator.EQ, "");
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field DUMMY on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("No such method error tested");
        }

        String expectedClassLogginError = "it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.ScalarOpValueBuilder";
        filter = new Filter("isee", FilterOperator.EQ, "abc");
        ((Logger) LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.OFF);
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Unsupported value provided for the field isee: it is supposed to be a %s", BigDecimal.class), e.getMessage());
            System.out.println("Unsupported value tested");
        } finally {
            ((Logger) LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.INFO);
        }

        filter = new Filter("isee()", FilterOperator.EQ, "123");
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field isee() on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("Method not found tested");
        }

        filter = new Filter("(java.lang.String)isee", FilterOperator.EQ, "123");
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("The Class defined as cast of the field isee of %s is not assignable to field type %s", OnboardingDroolsDTO.class, BigDecimal.class.getName()), e.getMessage());
            System.out.println("Class cast exception during implicit cast tested");
        }

        filter = new Filter("isee", FilterOperator.INSTANCE_OF, String.class.getName());
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Unsupported Class provided for the field isee", e.getMessage());
            System.out.println("Explicit Class cast exception tested");
        }

        filter = new Filter("(java.lang.String)isee", FilterOperator.EQ, "DUMMY");
        try {
            extraFilter2DroolsTransformerFacade.apply(filter, OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("The Class defined as cast of the field isee of " + OnboardingDroolsDTO.class + " is not assignable to field type java.math.BigDecimal", e.getMessage());
            System.out.println("Implicit Class cast exception tested");
        }
    }

    @Test
    void testImplicitCast() {
        String result = extraFilter2DroolsTransformerFacade.apply(new Filter("(" + LocalDateTime.class.getName() + ")criteriaConsensusTimestamp.(java.time.Month)month", FilterOperator.EQ, "JANUARY"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("((criteriaConsensusTimestamp instanceof " + LocalDateTime.class.getName() + ") && (criteriaConsensusTimestamp.month instanceof java.time.Month) && criteriaConsensusTimestamp.month == java.time.Month.valueOf(\"JANUARY\"))", result);
    }

    @Test
    void testCollection() {
        ExtraFilterTestModelSample sample = new ExtraFilterTestModelSample();
        sample.setStringObject("AGENDAGROUP");
        sample.setBooleanObject(false);
        sample.setCollectionObject(new ArrayList<>());
        sample.getCollectionObject().add("KEY1");
        sample.getCollectionObject().add("KEY2");
        sample.getCollectionObject().add("KEY3");

        DroolsRule rule = new DroolsRule();
        rule.setId(sample.getStringObject());
        rule.setName("CollectionTest");
        String ruleConsequence = "$sample.setBooleanObject(true);";

        System.out.println("Testing Collection EQ value matching");
        String result = extraFilter2DroolsTransformerFacade.apply(new Filter("collectionObject", FilterOperator.EQ, "KEY1"), ExtraFilterTestModelSample.class, null);
        Assertions.assertEquals("collectionObject contains \"KEY1\"", result);
        String ruleCondition = String.format("$sample: %s(%s)", ExtraFilterTestModelSample.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(sample, rule, true);
        sample.setBooleanObject(false);

        System.out.println("Testing Collection EQ value not matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("collectionObject", FilterOperator.EQ, "KEY4"), ExtraFilterTestModelSample.class, null);
        Assertions.assertEquals("collectionObject contains \"KEY4\"", result);
        ruleCondition = String.format("$sample: %s(%s)", ExtraFilterTestModelSample.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(sample, rule, false);
        sample.setBooleanObject(false);

        System.out.println("Testing Collection EQ collection matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("(java.util.ArrayList)collectionObject", FilterOperator.EQ, "(KEY1,KEY2,KEY3)"), ExtraFilterTestModelSample.class, null);
        Assertions.assertEquals("((collectionObject instanceof java.util.ArrayList) && collectionObject == new java.util.ArrayList(java.util.Arrays.asList(\"KEY1\",\"KEY2\",\"KEY3\")))", result);
        ruleCondition = String.format("$sample: %s(%s)", ExtraFilterTestModelSample.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(sample, rule, true);
        sample.setBooleanObject(false);

        System.out.println("Testing Collection EQ collection not matching");
        result = extraFilter2DroolsTransformerFacade.apply(new Filter("(java.util.ArrayList)collectionObject", FilterOperator.EQ, "(KEY1)"), ExtraFilterTestModelSample.class, null);
        Assertions.assertEquals("((collectionObject instanceof java.util.ArrayList) && collectionObject == new java.util.ArrayList(java.util.Arrays.asList(\"KEY1\")))", result);
        ruleCondition = String.format("$sample: %s(%s)", ExtraFilterTestModelSample.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(sample, rule, false);
        sample.setBooleanObject(false);

        result = extraFilter2DroolsTransformerFacade.apply(new Filter("(java.util.ArrayList)collectionObject", FilterOperator.EQ, "(KEY1,KEY2,KEY4)"), ExtraFilterTestModelSample.class, null);
        Assertions.assertEquals("((collectionObject instanceof java.util.ArrayList) && collectionObject == new java.util.ArrayList(java.util.Arrays.asList(\"KEY1\",\"KEY2\",\"KEY4\")))", result);
        ruleCondition = String.format("$sample: %s(%s)", ExtraFilterTestModelSample.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(sample, rule, false);
        sample.setBooleanObject(false);
    }

    void checkCollectionResult(ExtraFilterTestModelSample sample, DroolsRule rule, boolean success) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNOREDRULE");
        ignoredRule.setRule(applyRuleTemplate(ignoredRule.getId(), ignoredRule.getName(), "eval(true)", "throw new RuntimeException(\"This should not occur\");"));

        KieBase kieBase = new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule, ignoredRule)).block();
        Assertions.assertNotNull(kieBase);

        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(sample),
                new AgendaGroupSetFocusCommand(sample.getStringObject())
        );
        kieBase.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));

        Assertions.assertEquals(success,
                sample.getBooleanObject(), "Unexpected result applying rule%n%s%non object:%n%s".formatted(rule.getRule(), sample));
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

