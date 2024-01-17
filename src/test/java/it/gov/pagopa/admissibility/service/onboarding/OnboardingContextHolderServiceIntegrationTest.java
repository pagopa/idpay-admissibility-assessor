package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.RuleEngineService;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.common.redis.config.EmbeddedRedisTestConfiguration;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@TestPropertySource(
        properties = {
                "spring.redis.enabled=true",
        }
)
@ContextConfiguration(classes = EmbeddedRedisTestConfiguration.class)
class OnboardingContextHolderServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private KieContainerBuilderService kieContainerBuilderService;
    @Autowired
    private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    @Autowired
    private OnboardingContextHolderServiceImpl onboardingContextHolderService;
    @Autowired
    private RuleEngineService ruleEngineService;

    @Test
    void testKieBuildWithRedisCache() {

        // Assert the starting built rule size is 0
        int startingRuleBuiltSize = getRuleBuiltSize(onboardingContextHolderService);

        Assertions.assertEquals(0, startingRuleBuiltSize);

        // Caching invalid rules
        reactiveRedisTemplate.opsForValue().set(OnboardingContextHolderServiceImpl.ONBOARDING_CONTEXT_HOLDER_CACHE_NAME, "INVALIDOBJECT".getBytes()).block();
        refreshAndAssertKieContainerRuleSize(0);

        // Build a valid KieBase that produces a rule of size 1, assert KieBase is null after Reflection
        List<OnboardingRejectionReason> expectedRejectionReason =
                Collections.singletonList(
                        OnboardingRejectionReason.builder()
                                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.valueOf("INVALID_REQUEST"))
                                .code("OK")
                                .build()
                );

        DroolsRule dr = new DroolsRule();
        dr.setId("NAME");
        dr.setName("INITIATIVEID");
        dr.setRule("""
                package %s;

                rule "%s"
                agenda-group "%s"
                when $onb: %s()
                then $onb.getOnboardingRejectionReasons().add(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.builder().type(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.OnboardingRejectionReasonType.valueOf("INVALID_REQUEST")).code("OK").build());
                end
                """.formatted(
                        KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                        dr.getRule(),
                        dr.getName(),
                        OnboardingDroolsDTO.class.getName()
                )
        );

        KieBase kieBase = kieContainerBuilderService.build(Flux.just(dr)).block();
        onboardingContextHolderService.setBeneficiaryRulesKieBase(kieBase);
        TestUtils.waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(OnboardingContextHolderServiceImpl.ONBOARDING_CONTEXT_HOLDER_CACHE_NAME).block()) != null,
                ()->"KieBase not saved in cache",
                10,
                500
        );

        setContextHolderFieldToNull("kieBase");
        setContextHolderFieldToNull("kieBaseSerialized");

        Assertions.assertNull(onboardingContextHolderService.getBeneficiaryRulesKieBase());

        // Refresh KieBase and assert the built rules have expected size
        refreshAndAssertKieContainerRuleSize(1);

        // Execute rule and assert onboarding has the expected rejection reason
        OnboardingDTO onboardingMock = OnboardingDTOFaker.mockInstance(1, "INITIATIVEID");
        EvaluationDTO result = executeRules(onboardingMock);

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(expectedRejectionReason, resultCompleted.getOnboardingRejectionReasons());

        // Set a null kieBase
        onboardingContextHolderService.setBeneficiaryRulesKieBase(null);
        TestUtils.waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(OnboardingContextHolderServiceImpl.ONBOARDING_CONTEXT_HOLDER_CACHE_NAME).block()) == null,
                ()->"KieBase not saved in cache",
                10,
                500
        );

        onboardingContextHolderService.refreshKieContainer();
        TestUtils.waitFor(
                ()-> (reactiveRedisTemplate.opsForValue().get(OnboardingContextHolderServiceImpl.ONBOARDING_CONTEXT_HOLDER_CACHE_NAME).block()) != null,
                ()-> "KieBase is null",
                10,
                500
        );

        KieBase resultKieBase = onboardingContextHolderService.getBeneficiaryRulesKieBase();
        Assertions.assertNotNull(resultKieBase);

        int resultRuleBuiltSize = getRuleBuiltSize(onboardingContextHolderService);
        Assertions.assertEquals(0, resultRuleBuiltSize);
    }

    private void refreshAndAssertKieContainerRuleSize(int expectedRuleSize) {
        onboardingContextHolderService.refreshKieContainer();
        TestUtils.waitFor(
                ()-> onboardingContextHolderService.getBeneficiaryRulesKieBase() != null,
                ()->"KieBase is null",
                10,
                500
        );

        int ruleBuiltSize = getRuleBuiltSize(onboardingContextHolderService);
        Assertions.assertEquals(expectedRuleSize, ruleBuiltSize);
    }

    private void setContextHolderFieldToNull(String fieldName) {
        Field field = ReflectionUtils.findField(OnboardingContextHolderServiceImpl.class, fieldName);
        Assertions.assertNotNull(field);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, onboardingContextHolderService, null);
    }

    private EvaluationDTO executeRules(OnboardingDTO onb) {

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(onb.getInitiativeId())
                .beneficiaryInitiativeBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
                .initiativeName("NAME")
                .initiativeBudget(BigDecimal.valueOf(100))
                .status("STATUS")
                .automatedCriteriaCodes(List.of("CODE1"))
                .organizationId("ORGANIZATION-ID")
                .startDate(LocalDate.MIN)
                .automatedCriteria(List.of(new AutomatedCriteriaDTO()))
                .build();

        return ruleEngineService.applyRules(onb, config);
    }

    public static int getRuleBuiltSize(OnboardingContextHolderService onboardingContextHolderServiceSpy) {
        KieBase kieBase = onboardingContextHolderServiceSpy.getBeneficiaryRulesKieBase();
        if (kieBase == null) {
            return 0;
        } else {
            KiePackage kiePackage = kieBase.getKiePackage(KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE);
            return kiePackage != null
                    ? kiePackage.getRules().size()
                    : 0;
        }
    }
}
