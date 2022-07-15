package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorAnd;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorOr;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator.Aggregator2DroolsTransformerImpl;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.Filter2DroolsTranformerImpl;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.InOpValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.InstanceOfOpValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.ScalarOpValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.not.NotOperation2DroolsTransformerImpl;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImplTest;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ExtraFilter2DroolsTransformerImplTest {
    private static final BigDecimal ISEE = BigDecimal.valueOf(123);
    private static final OffsetDateTime ONBOARDING_DATE = OffsetDateTime.now();
    private static final String STATUS = "STATUS";
    private static final String USER_ID = "USERID";

    private static final ScalarOpValueBuilder scalarOpValueBuilder = new ScalarOpValueBuilder();

    public static ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer =
            new ExtraFilter2DroolsTransformerImpl(
                    new Aggregator2DroolsTransformerImpl()
                    , new NotOperation2DroolsTransformerImpl()
                    , new Filter2DroolsTranformerImpl(
                    new InstanceOfOpValueBuilder(),
                    scalarOpValueBuilder,
                    new InOpValueBuilder(scalarOpValueBuilder)
            ));

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
    }

    @Test
    public void testSuccessful() {
        String dayOfWeek = ONBOARDING_DATE.plusDays(1).getDayOfWeek().name();
        ExtraFilter extraFilter = new AggregatorAnd(Arrays.asList(
                new Filter("isee", FilterOperator.GT, ISEE.subtract(BigDecimal.TEN).toString()),
                new Filter("isee", FilterOperator.GE, ISEE.subtract(BigDecimal.ONE).toString()),
                new AggregatorOr(Arrays.asList(
                        new AggregatorAnd(Arrays.asList(
                                new Filter("status", FilterOperator.INSTANCE_OF, String.class.getName()),
                                new Filter("status", FilterOperator.EQ, STATUS)
                        )),
                        new AggregatorAnd(List.of(
                                new Filter("userId", FilterOperator.IN, "(" + USER_ID + ",NOT_MATCH)")
                        ))
                )),
                new Filter("isee", FilterOperator.LE, ISEE.add(BigDecimal.ONE).toString()),
                new Filter("isee", FilterOperator.LT, ISEE.add(BigDecimal.TEN).toString()),
                new NotOperation(
                        new Filter("criteriaConsensusTimestamp.dayOfWeek", FilterOperator.NOT_EQ, dayOfWeek)
                )
        ));
        String result = extraFilter2DroolsTransformer.apply(extraFilter, OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("(isee > new java.math.BigDecimal(\"113\") && isee >= new java.math.BigDecimal(\"122\") && ((status instanceof " + String.class.getName() + " && status == \"STATUS\") || (userId in (\"USERID\",\"NOT_MATCH\"))) && isee <= new java.math.BigDecimal(\"124\") && isee < new java.math.BigDecimal(\"133\") && !(criteriaConsensusTimestamp.dayOfWeek != java.time.DayOfWeek.valueOf(\"" + dayOfWeek + "\")))", result);
    }

    @Test
    public void testErrors() {
        try {
            extraFilter2DroolsTransformer.apply(new Filter("DUMMY", FilterOperator.EQ, ""), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field DUMMY on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("No such method error tested");
        }

        String expectedClassLogginError="it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.ScalarOpValueBuilder";
        try {
            ((Logger)LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.OFF);
            extraFilter2DroolsTransformer.apply(new Filter("isee", FilterOperator.EQ, "abc"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Unsupported value provided for the field isee: it is supposed to be a %s", BigDecimal.class), e.getMessage());
            System.out.println("Unsupported value tested");
        } finally {
            ((Logger)LoggerFactory.getLogger(expectedClassLogginError)).setLevel(Level.INFO);
        }

        try {
            extraFilter2DroolsTransformer.apply(new Filter("isee()", FilterOperator.EQ, "123"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("Cannot find field isee() on %s", OnboardingDroolsDTO.class), e.getMessage());
            System.out.println("Method not found tested");
        }

        try {
            extraFilter2DroolsTransformer.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "123"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals(String.format("The Class defined as cast of the field isee of %s is not assignable to field type %s", OnboardingDroolsDTO.class, BigDecimal.class.getName()), e.getMessage());
            System.out.println("Class cast exception during implicit cast tested");
        }

        try {
            extraFilter2DroolsTransformer.apply(new Filter("isee", FilterOperator.INSTANCE_OF, String.class.getName()), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Unsupported Class provided for the field isee", e.getMessage());
            System.out.println("Explicit Class cast exception tested");
        }

        try {
            extraFilter2DroolsTransformer.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "DUMMY"), OnboardingDroolsDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("The Class defined as cast of the field isee of " + OnboardingDroolsDTO.class + " is not assignable to field type java.math.BigDecimal", e.getMessage());
            System.out.println("Implicit Class cast exception tested");
        }
    }

    @Test
    public void testImplicitCast() {
        String result = extraFilter2DroolsTransformer.apply(new Filter("(" + LocalDateTime.class.getName() + ")criteriaConsensusTimestamp.(java.time.Month)month", FilterOperator.EQ, "JANUARY"), OnboardingDroolsDTO.class, null);
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
        String result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY1"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY1\"", result);
        String ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, true);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ value not matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY4"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY4\"", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, false);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ collection matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2,KEY3)"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY2\",\"KEY1\",\"KEY3\"))", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, true);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        System.out.println("Testing Collection EQ collection not matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1)"), OnboardingDroolsDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY1\"))", result);
        ruleCondition = String.format("$onboarding: %s(%s)", OnboardingDroolsDTO.class.getName(), result);
        rule.setRule(applyRuleTemplate(rule.getId(), rule.getName(), ruleCondition, ruleConsequence));
        checkCollectionResult(onboarding, rule, false);
        onboarding.setOnboardingRejectionReasons(new ArrayList<>());

        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2,KEY4)"), OnboardingDroolsDTO.class, null);
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

