package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImplTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.KieBase;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
    }

    @Test
    void applyRules() {
        // Given
        OnboardingContextHolderService onboardingContextHolderServiceMock = Mockito.mock(OnboardingContextHolderServiceImpl.class);
        CriteriaCodeService criteriaCodeServiceMock = Mockito.mock(CriteriaCodeService.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();
        Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = new Onboarding2OnboardingDroolsMapper();

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderServiceMock, onboarding2EvaluationMapper, criteriaCodeServiceMock, onboarding2OnboardingDroolsMapper);

        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId("INITIATIVEID");

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");

        Mockito.when(onboardingContextHolderServiceMock.getBeneficiaryRulesKieBase()).thenReturn(buildContainer(onboardingDTO.getInitiativeId()));

        // When
        EvaluationDTO result = ruleEngineService.applyRules(onboardingDTO, initiativeConfig);

        // Then
        Mockito.verify(onboardingContextHolderServiceMock).getBeneficiaryRulesKieBase();

        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);
        Assertions.assertNotNull(result.getAdmissibilityCheckDate());
        Assertions.assertFalse(result.getAdmissibilityCheckDate().isAfter(LocalDateTime.now()));
        Assertions.assertTrue(result.getAdmissibilityCheckDate().isAfter(LocalDateTime.now().minusMinutes(2)));

        EvaluationCompletedDTO expected = new EvaluationCompletedDTO();
        expected.setInitiativeId(onboardingDTO.getInitiativeId());
        expected.setInitiativeName(initiativeConfig.getInitiativeName());
        expected.setOrganizationId(initiativeConfig.getOrganizationId());
        expected.setAdmissibilityCheckDate(result.getAdmissibilityCheckDate());
        expected.setStatus(OnboardingEvaluationStatus.ONBOARDING_KO);
        expected.setOnboardingRejectionReasons(Collections.singletonList(OnboardingRejectionReason.builder()
                .code("REASON1")
                .build()));

        Assertions.assertEquals(expected, result);
    }

    private KieBase buildContainer(String initiativeId) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNOREDRULE");
        ignoredRule.setRule(ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(ignoredRule.getId(), ignoredRule.getName(), "eval(true)", "throw new RuntimeException(\"This should not occur\");"));

        DroolsRule rule = new DroolsRule();
        rule.setId(initiativeId);
        rule.setName("RULE");
        rule.setRule(ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(rule.getId(), rule.getName(),
                "$onboarding: %s()".formatted(OnboardingDroolsDTO.class.getName()),
                "$onboarding.getOnboardingRejectionReasons().add(%s.builder().code(\"REASON1\").build());".formatted(OnboardingRejectionReason.class.getName())));

        return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule, ignoredRule)).block();
    }
}
