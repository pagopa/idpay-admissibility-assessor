package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

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
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    @Test
    public void testSuccessful(){
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
        String result = extraFilter2DroolsTransformer.apply(extraFilter, OnboardingDTO.class, null);
        Assertions.assertEquals("(isee > new java.math.BigDecimal(\"113\") && isee >= new java.math.BigDecimal(\"122\") && ((status instanceof "+String.class.getName()+" && status == \"STATUS\") || (userId in (\"USERID\",\"NOT_MATCH\"))) && isee <= new java.math.BigDecimal(\"124\") && isee < new java.math.BigDecimal(\"133\") && !(criteriaConsensusTimestamp.dayOfWeek != java.time.DayOfWeek.valueOf(\""+dayOfWeek+"\")))",result);
    }

    @Test
    public void testErrors(){
        try{
            extraFilter2DroolsTransformer.apply(new Filter("DUMMY", FilterOperator.EQ, ""), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals(String.format("Cannot find field DUMMY on %s", OnboardingDTO.class), e.getMessage());
            System.out.println("No such method error tested");
        }

        try{
            extraFilter2DroolsTransformer.apply(new Filter("isee", FilterOperator.EQ, "abc"), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals(String.format("Unsupported value provided for the field isee: it is supposed to be a %s", BigDecimal.class), e.getMessage());
            System.out.println("Unsupported value tested");
        }

        try{
            extraFilter2DroolsTransformer.apply(new Filter("isee()", FilterOperator.EQ, "123"), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals(String.format("Cannot find field isee() on %s", OnboardingDTO.class), e.getMessage());
            System.out.println("Method not found tested");
        }

        try{
            extraFilter2DroolsTransformer.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "123"), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals(String.format("The Class defined as cast of the field isee of %s is not assignable to field type %s", OnboardingDTO.class, BigDecimal.class.getName()), e.getMessage());
            System.out.println("Class cast exception during implicit cast tested");
        }

        try{
            extraFilter2DroolsTransformer.apply(new Filter("isee", FilterOperator.INSTANCE_OF, String.class.getName()), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals("Unsupported Class provided for the field isee", e.getMessage());
            System.out.println("Explicit Class cast exception tested");
        }

        try{
            extraFilter2DroolsTransformer.apply(new Filter("(java.lang.String)isee", FilterOperator.EQ, "DUMMY"), OnboardingDTO.class, null);
            Assertions.fail("Exception not thrown");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals("The Class defined as cast of the field isee of "+OnboardingDTO.class+" is not assignable to field type java.math.BigDecimal", e.getMessage());
            System.out.println("Implicit Class cast exception tested");
        }
    }

    @Test
    public void testImplicitCast(){
        String result = extraFilter2DroolsTransformer.apply(new Filter("(" + LocalDateTime.class.getName() + ")criteriaConsensusTimestamp.(java.time.Month)month", FilterOperator.EQ, "JANUARY"), OnboardingDTO.class, null);
        Assertions.assertEquals("((criteriaConsensusTimestamp instanceof "+LocalDateTime.class.getName()+") && (criteriaConsensusTimestamp.month instanceof java.time.Month) && criteriaConsensusTimestamp.month == java.time.Month.valueOf(\"JANUARY\"))", result);
    }

    @Test
    public void testCollection(){
        OnboardingDTO onboarding = new OnboardingDTO();
        onboarding.setSelfDeclarationList(new HashMap<>());
        onboarding.getSelfDeclarationList().put("KEY1", true);
        onboarding.getSelfDeclarationList().put("KEY2", false);
        onboarding.getSelfDeclarationList().put("KEY3", null);

        DroolsRule rule =new DroolsRule();
        rule.setName("CollectionTest");
        rule.setRuleConsequence("output.put(\"OK\", true);");

        System.out.println("Testing Collection EQ value matching");
        String result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY1"), OnboardingDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY1\"", result);
        rule.setRuleCondition(String.format("$onboarding: %s(%s)", OnboardingDTO.class.getName(), result));
        rule.setRule(null);
        checkCollectionResult(onboarding, rule, true);

        System.out.println("Testing Collection EQ value not matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "KEY4"), OnboardingDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() contains \"KEY4\"", result);
        rule.setRuleCondition(String.format("$onboarding: %s(%s)", OnboardingDTO.class.getName(), result));
        rule.setRule(null);
        checkCollectionResult(onboarding, rule, false);

        System.out.println("Testing Collection EQ collection matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2)"), OnboardingDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY2\",\"KEY1\"))", result);
        rule.setRuleCondition(String.format("$onboarding: %s(%s)", OnboardingDTO.class.getName(), result));
        rule.setRule(null);
        checkCollectionResult(onboarding, rule, true);

        System.out.println("Testing Collection EQ collection not matching");
        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1)"), OnboardingDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY1\"))", result);
        rule.setRuleCondition(String.format("$onboarding: %s(%s)", OnboardingDTO.class.getName(), result));
        rule.setRule(null);
        checkCollectionResult(onboarding, rule, false);

        result = extraFilter2DroolsTransformer.apply(new Filter("selfDeclarationList.keySet()", FilterOperator.EQ, "(KEY1,KEY2,KEY4)"), OnboardingDTO.class, null);
        Assertions.assertEquals("selfDeclarationList.keySet() == new java.util.HashSet(java.util.Arrays.asList(\"KEY2\",\"KEY1\",\"KEY4\"))", result);
        rule.setRuleCondition(String.format("$onboarding: %s(%s)", OnboardingDTO.class.getName(), result));
        rule.setRule(null);
        checkCollectionResult(onboarding, rule, false);
    }

    private void checkCollectionResult(OnboardingDTO onboardingDTO, DroolsRule rule, boolean success) {
        // TODO
//        Map<String, Object> results = new ExecuteDroolsEngineCommandImpl().execute(Collections.singleton(rule), Collections.singleton(onboardingDTO));
//        Assertions.assertNotNull(results);
//        Object result = results.get("OK");
//        if(success){
//            Assertions.assertEquals(Boolean.TRUE, result);
//        } else {
//            Assertions.assertNull(result);
//        }
    }
}

