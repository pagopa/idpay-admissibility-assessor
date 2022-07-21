package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.DataNascita;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.RuleEngineService;
import it.gov.pagopa.admissibility.service.onboarding.RuleEngineServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeneficiaryRule2DroolsRuleImplTest {

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final CriteriaCodeService criteriaCodeServiceMock;

    public BeneficiaryRule2DroolsRuleImplTest() {
        this.criteriaCodeServiceMock = Mockito.mock(CriteriaCodeService.class);
        this.beneficiaryRule2DroolsRule = buildBeneficiaryRule2DroolsRule(false);
    }

    private BeneficiaryRule2DroolsRuleImpl buildBeneficiaryRule2DroolsRule(boolean executeOnlineBuildCheck) {
        return new BeneficiaryRule2DroolsRuleImpl(executeOnlineBuildCheck, criteriaCodeServiceMock, ExtraFilter2DroolsTransformerFacadeImplTest.extraFilter2DroolsTransformerFacade, new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)));
    }

    @BeforeEach
    public void configureMock(){
        CriteriaCodeConfigFaker.mockedCriteriaCodes.forEach(c->{
            Mockito.when(criteriaCodeServiceMock.getCriteriaCodeConfig(c.getCode())).thenReturn(c);
        });
    }

    @Test
    public void testBuild() {
        // given
        Initiative2BuildDTO dto = buildInitiative();

        // when
        DroolsRule result = buildBeneficiaryRule2DroolsRule(true).apply(Flux.just(dto)).blockFirst();

        // then
        checkResult(result);
    }

    @Test
    public void testBuildWithOnlineBuildCheck() {
        // given
        Initiative2BuildDTO dto = buildInitiative();

        // when
        DroolsRule result = beneficiaryRule2DroolsRule.apply(Flux.just(dto)).blockFirst();

        // then
        checkResult(result);
    }

    private void checkResult(DroolsRule result) {
        DroolsRule expected = new DroolsRule();
        expected.setName("ID-NAME");
        expected.setId("ID");
        expected.setRule("""
                package it.gov.pagopa.admissibility.drools.buildrules;
                                        
                rule "ID-NAME-ISEE"
                agenda-group "ID"
                when $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(!(isee == new java.math.BigDecimal("1")))
                then $onboarding.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_ISEE_FAIL");
                end
                                        
                                        
                rule "ID-NAME-BIRTHDATE"
                agenda-group "ID"
                when $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(!(birthDate.anno > "2000"))
                then $onboarding.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_BIRTHDATE_FAIL");
                end
                                        
                """);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testExecutions() {
        testExecution(Collections.emptyList());
        testExecution(List.of("ISEE"));
        testExecution(List.of("BIRTHDATE"));
        testExecution(List.of("ISEE", "BIRTHDATE"));
    }

    public void testExecution(List<String> failingCode){
        //given
        boolean expectedIseeFail = failingCode.contains("ISEE");
        boolean expectedBirthDateFail = failingCode.contains("BIRTHDATE");

        Initiative2BuildDTO initiative = buildInitiative();

        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiative.getInitiativeId());
        onboardingDTO.setBirthDate(new DataNascita());

        if(expectedIseeFail){
            onboardingDTO.setIsee(BigDecimal.TEN);
        } else {
            onboardingDTO.setIsee(BigDecimal.ONE);
        }
        if(expectedBirthDateFail){
            onboardingDTO.getBirthDate().setAnno("2000");
        } else {
            onboardingDTO.getBirthDate().setAnno("2021");
        }

        DroolsRule rule = beneficiaryRule2DroolsRule.apply(Flux.just(initiative)).blockFirst();

        OnboardingContextHolderService onboardingContextHolderService=Mockito.mock(OnboardingContextHolderService.class);
        Mockito.when(onboardingContextHolderService.getBeneficiaryRulesKieContainer()).thenReturn(buildContainer(rule));

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderService, new Onboarding2EvaluationMapper(), new Onboarding2OnboardingDroolsMapper());

        // when
        EvaluationDTO evaluationResult = ruleEngineService.applyRules(onboardingDTO);

        // then
        Assertions.assertNotNull(rule);

        EvaluationDTO expectedEvaluationResult = new EvaluationDTO();
        expectedEvaluationResult.setInitiativeId(initiative.getInitiativeId());
        expectedEvaluationResult.setAdmissibilityCheckDate(evaluationResult.getAdmissibilityCheckDate());
        expectedEvaluationResult.setOnboardingRejectionReasons(new ArrayList<>());
        if(expectedIseeFail){
            expectedEvaluationResult.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_ISEE_FAIL");
        }
        if(expectedBirthDateFail){
            expectedEvaluationResult.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_BIRTHDATE_FAIL");
        }
        expectedEvaluationResult.setStatus(expectedEvaluationResult.getOnboardingRejectionReasons().size() == 0? "ONBOARDING_OK" : "ONBOARDING_KO");

        Assertions.assertEquals(expectedEvaluationResult, evaluationResult);
    }

    private Initiative2BuildDTO buildInitiative() {
        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("ID");
        dto.setInitiativeName("NAME");
        dto.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        List<AutomatedCriteriaDTO> criterias = new ArrayList<>();

        criterias.add(new AutomatedCriteriaDTO("AUTH1", "ISEE", null, FilterOperator.EQ, "1"));
        criterias.add(new AutomatedCriteriaDTO("AUTH2", "BIRTHDATE", "anno", FilterOperator.GT, "2000"));

        dto.getBeneficiaryRule().setAutomatedCriteria(criterias);
        return dto;
    }

    private KieContainer buildContainer(DroolsRule rule) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNOREDRULE");
        ignoredRule.setRule(ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(ignoredRule.getId(), ignoredRule.getName(), "eval(true)", "throw new RuntimeException(\"This should not occur\");"));

        return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule, ignoredRule)).block();
    }
}
