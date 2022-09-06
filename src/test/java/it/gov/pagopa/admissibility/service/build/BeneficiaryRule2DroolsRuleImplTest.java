package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.mapper.AutomatedCriteria2ExtraFilterMapper;
import it.gov.pagopa.admissibility.mapper.Initiative2InitiativeConfigMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.RuleEngineService;
import it.gov.pagopa.admissibility.service.onboarding.RuleEngineServiceImpl;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BeneficiaryRule2DroolsRuleImplTest {

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final CriteriaCodeService criteriaCodeServiceMock;

    private final Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper;

    public BeneficiaryRule2DroolsRuleImplTest() {
        this.criteriaCodeServiceMock = Mockito.mock(CriteriaCodeService.class);
        this.initiative2InitiativeConfigMapper = new Initiative2InitiativeConfigMapper();
        this.beneficiaryRule2DroolsRule = buildBeneficiaryRule2DroolsRule(false);
    }

    private BeneficiaryRule2DroolsRuleImpl buildBeneficiaryRule2DroolsRule(boolean executeOnlineBuildCheck) {
        return new BeneficiaryRule2DroolsRuleImpl(executeOnlineBuildCheck, initiative2InitiativeConfigMapper, criteriaCodeServiceMock, new AutomatedCriteria2ExtraFilterMapper(), ExtraFilter2DroolsTransformerFacadeImplTest.extraFilter2DroolsTransformerFacade, new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)));
    }

    @BeforeEach
    public void configureMock() {
        CriteriaCodeConfigFaker.mockedCriteriaCodes.forEach(c -> Mockito.when(criteriaCodeServiceMock.getCriteriaCodeConfig(c.getCode())).thenReturn(c));
    }

    @Test
    void testBuild() {
        // given
        Initiative2BuildDTO dto = buildInitiative();

        // when
        DroolsRule result = buildBeneficiaryRule2DroolsRule(true).apply(dto);

        // then
        checkResult(result);
    }

    @Test
    void testBuildWithOnlineBuildCheck() {
        // given
        Initiative2BuildDTO dto = buildInitiative();

        // when
        DroolsRule result = beneficiaryRule2DroolsRule.apply(dto);

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
                when
                   $criteriaCodeService: it.gov.pagopa.admissibility.service.CriteriaCodeService()
                   $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(!(isee == new java.math.BigDecimal("1")))
                then
                   it.gov.pagopa.admissibility.model.CriteriaCodeConfig criteriaCodeConfig = $criteriaCodeService.getCriteriaCodeConfig("ISEE");
                   $onboarding.getOnboardingRejectionReasons().add(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.builder().type(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.OnboardingRejectionReasonType.valueOf("AUTOMATED_CRITERIA_FAIL")).code("AUTOMATED_CRITERIA_ISEE_FAIL").authority(criteriaCodeConfig.getAuthority()).authorityLabel(criteriaCodeConfig.getAuthorityLabel()).build());
                end
                                        
                                        
                rule "ID-NAME-BIRTHDATE"
                agenda-group "ID"
                when
                   $criteriaCodeService: it.gov.pagopa.admissibility.service.CriteriaCodeService()
                   $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(!(birthDate.year > "2000"))
                then
                   it.gov.pagopa.admissibility.model.CriteriaCodeConfig criteriaCodeConfig = $criteriaCodeService.getCriteriaCodeConfig("BIRTHDATE");
                   $onboarding.getOnboardingRejectionReasons().add(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.builder().type(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.OnboardingRejectionReasonType.valueOf("AUTOMATED_CRITERIA_FAIL")).code("AUTOMATED_CRITERIA_BIRTHDATE_FAIL").authority(criteriaCodeConfig.getAuthority()).authorityLabel(criteriaCodeConfig.getAuthorityLabel()).build());
                end
                                        
                """);

        expected.setInitiativeConfig(InitiativeConfig.builder()
                .initiativeId("ID")
                .initiativeName("NAME")
                .organizationId("ORGANIZATIONID")
                .startDate(LocalDate.of(2021, 1, 1))
                .endDate(LocalDate.of(2025, 12, 1))
                .pdndToken("PDND_TOKEN")
                .automatedCriteriaCodes(List.of("ISEE", "BIRTHDATE"))
                .initiativeBudget(new BigDecimal("100000.00"))
                .beneficiaryInitiativeBudget(new BigDecimal("1000.00"))
                .build());

        Assertions.assertEquals(expected, result);
    }

    @Test
    void testExecutions() {
        testExecution(Collections.emptyList());
        testExecution(List.of("ISEE"));
        testExecution(List.of("BIRTHDATE"));
        testExecution(List.of("ISEE", "BIRTHDATE"));
    }

    void testExecution(List<String> failingCode) {
        //given
        boolean expectedIseeFail = failingCode.contains("ISEE");
        boolean expectedBirthDateFail = failingCode.contains("BIRTHDATE");

        Initiative2BuildDTO initiative = buildInitiative();

        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiative.getInitiativeId());
        onboardingDTO.setBirthDate(new BirthDate());

        if (expectedIseeFail) {
            onboardingDTO.setIsee(BigDecimal.TEN);
        } else {
            onboardingDTO.setIsee(BigDecimal.ONE);
        }
        if (expectedBirthDateFail) {
            onboardingDTO.getBirthDate().setYear("2000");
        } else {
            onboardingDTO.getBirthDate().setYear("2021");
        }

        DroolsRule rule = beneficiaryRule2DroolsRule.apply(initiative);

        OnboardingContextHolderService onboardingContextHolderService = Mockito.mock(OnboardingContextHolderService.class);
        Mockito.when(onboardingContextHolderService.getBeneficiaryRulesKieContainer()).thenReturn(buildContainer(rule));

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderService, new Onboarding2EvaluationMapper(), criteriaCodeServiceMock, new Onboarding2OnboardingDroolsMapper());

        // when
        EvaluationDTO evaluationResult = ruleEngineService.applyRules(onboardingDTO, initiative2InitiativeConfigMapper.apply(initiative));

        // then
        Assertions.assertNotNull(rule);

        EvaluationDTO expectedEvaluationResult = new EvaluationDTO();
        expectedEvaluationResult.setInitiativeId(initiative.getInitiativeId());
        expectedEvaluationResult.setInitiativeName("NAME");
        expectedEvaluationResult.setOrganizationId("ORGANIZATIONID");
        expectedEvaluationResult.setAdmissibilityCheckDate(evaluationResult.getAdmissibilityCheckDate());
        expectedEvaluationResult.setOnboardingRejectionReasons(new ArrayList<>());
        if (expectedIseeFail) {
            expectedEvaluationResult.getOnboardingRejectionReasons().add(OnboardingRejectionReason.builder()
                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                    .code("AUTOMATED_CRITERIA_ISEE_FAIL")
                    .authority("INPS")
                    .authorityLabel("Istituto Nazionale Previdenza Sociale")
                    .build());
        }
        if (expectedBirthDateFail) {
            expectedEvaluationResult.getOnboardingRejectionReasons().add(OnboardingRejectionReason.builder()
                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                    .code("AUTOMATED_CRITERIA_BIRTHDATE_FAIL")
                    .authority("AGID")
                    .authorityLabel("Agenzia per l'Italia Digitale")
                    .build());
        }
        expectedEvaluationResult.setStatus(expectedEvaluationResult.getOnboardingRejectionReasons().size() == 0 ? "ONBOARDING_OK" : "ONBOARDING_KO");

        Assertions.assertEquals(expectedEvaluationResult, evaluationResult);
    }

    private Initiative2BuildDTO buildInitiative() {
        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("ID");
        dto.setInitiativeName("NAME");
        dto.setOrganizationId("ORGANIZATIONID");
        dto.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        List<AutomatedCriteriaDTO> criterias = new ArrayList<>();

        criterias.add(new AutomatedCriteriaDTO("AUTH1", "ISEE", null, FilterOperator.EQ, "1", null));
        criterias.add(new AutomatedCriteriaDTO("AUTH2", "BIRTHDATE", "year", FilterOperator.GT, "2000", null));

        dto.getBeneficiaryRule().setAutomatedCriteria(criterias);
        dto.setPdndToken("PDND_TOKEN");
        dto.setGeneral(new InitiativeGeneralDTO("NAME", new BigDecimal("100000.00"),
                InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, Boolean.TRUE, new BigDecimal("1000.00"),
                LocalDate.of(2021, 1, 1), LocalDate.of(2025, 12, 1),
                null, null));

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
