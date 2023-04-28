package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.AutomatedCriteria2ExtraFilterMapper;
import it.gov.pagopa.admissibility.mapper.Initiative2InitiativeConfigMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.RuleEngineService;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.RuleEngineServiceImpl;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 ******************
 For any change necessary on this test consider if update "ruleVersion" value setted in it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl.apply
 ******************
*/
class BeneficiaryRule2DroolsRuleImplTest {
    public static final String ENCRYPTED_API_KEY_CLIENT_ID = "ENCRYPTED_API_KEY_CLIENT_ID";
    public static final String ENCRYPTED_API_KEY_CLIENT_ASSERTION = "ENCRYPTED_API_KEY_CLIENT_ASSERTION";

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
        checkResult(result, dto);
    }

    @Test
    void testBuildWithOnlineBuildCheck() {
        // given
        Initiative2BuildDTO dto = buildInitiative();

        // when
        DroolsRule result = beneficiaryRule2DroolsRule.apply(dto);

        // then
        checkResult(result, dto);
    }

    private void checkResult(DroolsRule result, Initiative2BuildDTO dto) {
        DroolsRule expected = new DroolsRule();
        expected.setName("NAME");
        expected.setId("ID");
        expected.setRule("""
                package it.gov.pagopa.admissibility.drools.buildrules;
                
                // NAME
                // ruleVersion: 20230404
                
                rule "ID-ISEE"
                no-loop true
                agenda-group "ID"
                when
                   $criteriaCodeService: it.gov.pagopa.admissibility.service.CriteriaCodeService()
                   $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(initiativeId == "ID", !(isee == new java.math.BigDecimal("1")))
                then
                   it.gov.pagopa.admissibility.model.CriteriaCodeConfig criteriaCodeConfig = $criteriaCodeService.getCriteriaCodeConfig("ISEE");
                   $onboarding.getOnboardingRejectionReasons().add(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.builder().type(it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason.OnboardingRejectionReasonType.valueOf("AUTOMATED_CRITERIA_FAIL")).code("AUTOMATED_CRITERIA_ISEE_FAIL").authority(criteriaCodeConfig.getAuthority()).authorityLabel(criteriaCodeConfig.getAuthorityLabel()).build());
                end
                                        
                                        
                rule "ID-BIRTHDATE"
                no-loop true
                agenda-group "ID"
                when
                   $criteriaCodeService: it.gov.pagopa.admissibility.service.CriteriaCodeService()
                   $onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO(initiativeId == "ID", !(birthDate.year > "2000"))
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
                .automatedCriteria(dto.getBeneficiaryRule().getAutomatedCriteria())
                .apiKeyClientId(ENCRYPTED_API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(ENCRYPTED_API_KEY_CLIENT_ASSERTION)
                .automatedCriteriaCodes(List.of("ISEE", "BIRTHDATE"))
                .initiativeBudget(new BigDecimal("100000.00"))
                .beneficiaryInitiativeBudget(new BigDecimal("1000.00"))
                .isLogoPresent(Boolean.TRUE)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build());

        expected.setRuleVersion("20230404");

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
        Mockito.when(onboardingContextHolderService.getBeneficiaryRulesKieBase()).thenReturn(buildKieBase(rule));

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderService, new Onboarding2EvaluationMapper(), criteriaCodeServiceMock, new Onboarding2OnboardingDroolsMapper());

        // when
        EvaluationDTO evaluationResult = ruleEngineService.applyRules(onboardingDTO, initiative2InitiativeConfigMapper.apply(initiative));

        // then
        Assertions.assertNotNull(rule);

        EvaluationCompletedDTO expectedEvaluationResult = new EvaluationCompletedDTO();
        expectedEvaluationResult.setInitiativeId(initiative.getInitiativeId());
        expectedEvaluationResult.setInitiativeName("NAME");
        expectedEvaluationResult.setOrganizationId("ORGANIZATIONID");
        expectedEvaluationResult.setAdmissibilityCheckDate(evaluationResult.getAdmissibilityCheckDate());
        expectedEvaluationResult.setInitiativeEndDate(LocalDate.of(2025, 12, 1));
        expectedEvaluationResult.setBeneficiaryBudget(new BigDecimal("1000.00"));
        expectedEvaluationResult.setIsLogoPresent(Boolean.TRUE);
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
        expectedEvaluationResult.setStatus(expectedEvaluationResult.getOnboardingRejectionReasons().size() == 0 ? OnboardingEvaluationStatus.ONBOARDING_OK : OnboardingEvaluationStatus.ONBOARDING_KO);

        Assertions.assertEquals(expectedEvaluationResult, evaluationResult);
    }

    private Initiative2BuildDTO buildInitiative() {
        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("ID");
        dto.setInitiativeName("NAME");
        dto.setOrganizationId("ORGANIZATIONID");
        dto.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        List<AutomatedCriteriaDTO> criterias = new ArrayList<>();

        List<IseeTypologyEnum> typology = List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);
        criterias.add(new AutomatedCriteriaDTO("AUTH1", "ISEE", null, FilterOperator.EQ, "1", null, Sort.Direction.ASC, typology));
        criterias.add(new AutomatedCriteriaDTO("AUTH2", "BIRTHDATE", "year", FilterOperator.GT, "2000", null, null, typology));

        dto.getBeneficiaryRule().setAutomatedCriteria(criterias);
        dto.getBeneficiaryRule().setApiKeyClientId(ENCRYPTED_API_KEY_CLIENT_ID);
        dto.getBeneficiaryRule().setApiKeyClientAssertion(ENCRYPTED_API_KEY_CLIENT_ASSERTION);
        dto.setGeneral(
                InitiativeGeneralDTO.builder()
                        .name("NAME")
                        .budget(new BigDecimal("100000.00"))
                        .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                        .beneficiaryKnown(Boolean.TRUE)
                        .beneficiaryBudget(new BigDecimal("1000.00"))
                        .startDate(LocalDate.of(2021, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 1))
                        .build()
    );

        dto.setAdditionalInfo(new InitiativeAdditionalInfoDTO(
                "SERVICENAME",
                "ARGUMENT",
                "DESCRIPTION",
                List.of(ChannelsDTO.builder().type("web").contact("CONTACT").build()),
                "logo.png"
        ));

        return dto;
    }

    private KieBase buildKieBase(DroolsRule rule) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNOREDRULE");
        ignoredRule.setRule(ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(ignoredRule.getId(), ignoredRule.getName(), "eval(true)", "throw new RuntimeException(\"This should not occur\");"));

        return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(rule, ignoredRule)).block();
    }
}
