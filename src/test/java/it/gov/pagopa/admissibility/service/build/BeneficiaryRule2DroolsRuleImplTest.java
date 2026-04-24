package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.AutomatedCriteria2ExtraFilterMapper;
import it.gov.pagopa.admissibility.mapper.Initiative2InitiativeConfigMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
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
import java.util.Set;

/*
 ******************
 For any change necessary on this test consider if update "ruleVersion" value setted in it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl.apply
 ******************
*/
class BeneficiaryRule2DroolsRuleImplTest {

    private final CriteriaCodeService criteriaCodeServiceMock =
            Mockito.mock(CriteriaCodeService.class);

    private final Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper =
            new Initiative2InitiativeConfigMapper();

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule =
            new BeneficiaryRule2DroolsRuleImpl(
                    false,
                    initiative2InitiativeConfigMapper,
                    criteriaCodeServiceMock,
                    new AutomatedCriteria2ExtraFilterMapper(),
                    ExtraFilter2DroolsTransformerFacadeImplTest.extraFilter2DroolsTransformerFacade,
                    new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class))
            );

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.mockedCriteriaCodes.forEach(c ->
                Mockito.when(criteriaCodeServiceMock.getCriteriaCodeConfig(c.getCode()))
                        .thenReturn(c)
        );
    }

    @Test
    void testBuild() {
        Initiative2BuildDTO dto = buildInitiative();

        DroolsRule result = beneficiaryRule2DroolsRule.apply(dto);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ID", result.getId());
        Assertions.assertEquals("NAME", result.getName());
        Assertions.assertEquals("20230404", result.getRuleVersion());
        Assertions.assertNotNull(result.getRule());
        Assertions.assertNotNull(result.getInitiativeConfig());
    }

    @Test
    void testExecution_allCases() {
        testExecution(Collections.emptyList());
        testExecution(List.of("isee"));
        testExecution(List.of("birthdate"));
        testExecution(List.of("isee", "birthdate"));
        testExecution(List.of("notready"));
    }

    private void testExecution(List<String> failingCodes) {

        boolean expectedIseeFail = failingCodes.contains("isee");
        boolean expectedBirthDateFail = failingCodes.contains("birthdate");
        boolean expectedNotReady = failingCodes.equals(List.of("notready"));

        Initiative2BuildDTO initiative = buildInitiative();
        DroolsRule rule = beneficiaryRule2DroolsRule.apply(initiative);

        OnboardingDTO onboardingDTO = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId(initiative.getInitiativeId())
                .birthDate(new BirthDate())
                .verifies(new ArrayList<>())
                .build();

        onboardingDTO.setIsee(expectedIseeFail ? BigDecimal.TEN : BigDecimal.ONE);
        onboardingDTO.getBirthDate()
                .setYear(expectedBirthDateFail ? "2000" : "2021");

        OnboardingContextHolderService contextHolderService =
                Mockito.mock(OnboardingContextHolderService.class);

        Mockito.when(contextHolderService.getBeneficiaryRulesKieBase())
                .thenReturn(buildKieBase(rule));

        Mockito.when(contextHolderService.getBeneficiaryRulesKieInitiativeIds())
                .thenReturn(expectedNotReady
                        ? Collections.emptySet()
                        : Set.of(initiative.getInitiativeId()));

        RuleEngineService ruleEngineService =
                new RuleEngineServiceImpl(
                        contextHolderService,
                        new Onboarding2EvaluationMapper(),
                        criteriaCodeServiceMock,
                        new Onboarding2OnboardingDroolsMapper()
                );

        EvaluationDTO evaluation =
                ruleEngineService.applyRules(
                        onboardingDTO,
                        initiative2InitiativeConfigMapper.apply(initiative)
                );

        Assertions.assertNotNull(evaluation);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, evaluation);

        EvaluationCompletedDTO completed = (EvaluationCompletedDTO) evaluation;

        if (expectedIseeFail || expectedBirthDateFail || expectedNotReady) {
            Assertions.assertEquals(
                    OnboardingEvaluationStatus.ONBOARDING_KO,
                    completed.getStatus()
            );
        } else {
            Assertions.assertEquals(
                    OnboardingEvaluationStatus.ONBOARDING_OK,
                    completed.getStatus()
            );
        }
    }

    private Initiative2BuildDTO buildInitiative() {

        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("ID");
        dto.setInitiativeName("NAME");
        dto.setOrganizationId("ORGANIZATIONID");

        List<IseeTypologyEnum> typologies =
                List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);

        List<AutomatedCriteriaDTO> automatedCriteria = List.of(
                new AutomatedCriteriaDTO(
                        "AUTH1", "isee", null,
                        FilterOperator.EQ, "1",
                        null, Sort.Direction.ASC,
                        typologies,
                        new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID_ISEE")
                ),
                new AutomatedCriteriaDTO(
                        "AUTH2", "birthdate", "year",
                        FilterOperator.GT, "2000",
                        null, null,
                        typologies,
                        new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID_BIRTHDATE")
                )
        );

        dto.setBeneficiaryRule(
                InitiativeBeneficiaryRuleDTO.builder()
                        .automatedCriteria(automatedCriteria)
                        .build()
        );

        dto.setGeneral(
                InitiativeGeneralDTO.builder()
                        .name("NAME")
                        .budgetCents(100_000_00L)
                        .beneficiaryBudgetFixedCents(1_000_00L)
                        .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                        .startDate(LocalDate.of(2021, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 1))
                        .build()
        );

        dto.setAdditionalInfo(
                new InitiativeAdditionalInfoDTO(
                        "SERVICENAME",
                        "ARGUMENT",
                        "DESCRIPTION",
                        List.of(ChannelsDTO.builder().type("web").contact("CONTACT").build()),
                        "logo.png"
                )
        );

        return dto;
    }

    private KieBase buildKieBase(DroolsRule rule) {
        DroolsRule ignoredRule = new DroolsRule();
        ignoredRule.setId("IGNORED");
        ignoredRule.setName("IGNORED_RULE");
        ignoredRule.setRule(
                ExtraFilter2DroolsTransformerFacadeImplTest.applyRuleTemplate(
                        ignoredRule.getId(),
                        ignoredRule.getName(),
                        "eval(true)",
                        "throw new RuntimeException(\"This should not occur\");"
                )
        );

        return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class))
                .build(Flux.just(rule, ignoredRule))
                .block();
    }
}