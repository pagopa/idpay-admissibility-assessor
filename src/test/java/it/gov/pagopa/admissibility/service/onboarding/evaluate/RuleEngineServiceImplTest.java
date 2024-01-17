package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacadeImplTest;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImplTest;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.KieBase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    @Mock private OnboardingContextHolderService onboardingContextHolderServiceMock;
    @Mock private CriteriaCodeService criteriaCodeServiceMock;

    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();
    private final Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = new Onboarding2OnboardingDroolsMapper();

    private RuleEngineService ruleEngineService;

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
    }

    @BeforeEach
    void init(){
        ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderServiceMock, onboarding2EvaluationMapper, criteriaCodeServiceMock, onboarding2OnboardingDroolsMapper);
    }

    @AfterEach
    void verifyNotMoreInvocations(){
        Mockito.verifyNoMoreInteractions(
                onboardingContextHolderServiceMock,
                criteriaCodeServiceMock);
    }

    @Test
    void applyRules() {
        // Given
        String initiativeId = INITIATIVEID;
        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiativeId);

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId(initiativeId);
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");

        initiativeConfig.setAutomatedCriteria(List.of(new AutomatedCriteriaDTO()));

        Mockito.when(onboardingContextHolderServiceMock.getBeneficiaryRulesKieBase())
                .thenReturn(buildContainer(initiativeId));

        Mockito.when(onboardingContextHolderServiceMock.getBeneficiaryRulesKieInitiativeIds())
                .thenReturn(Set.of(initiativeId));

        // When
        EvaluationDTO result = ruleEngineService.applyRules(onboardingDTO, initiativeConfig);

        // Then
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        Assertions.assertNotNull(result.getAdmissibilityCheckDate());
        Assertions.assertFalse(result.getAdmissibilityCheckDate().isAfter(LocalDateTime.now()));
        Assertions.assertTrue(result.getAdmissibilityCheckDate().isAfter(LocalDateTime.now().minusMinutes(2)));

        EvaluationCompletedDTO expected = new EvaluationCompletedDTO();
        expected.setInitiativeId(onboardingDTO.getInitiativeId());
        expected.setInitiativeName(initiativeConfig.getInitiativeName());
        expected.setOrganizationId(initiativeConfig.getOrganizationId());
        expected.setAdmissibilityCheckDate(result.getAdmissibilityCheckDate());
        expected.setStatus(OnboardingEvaluationStatus.ONBOARDING_KO);
        expected.getOnboardingRejectionReasons().add(OnboardingRejectionReason.builder()
                .code("REASON1")
                .build());

        Assertions.assertEquals(expected, result);
    }

    @Test
    void testNotRules_notKieBasedInitiatives(){
        // Given
        String initiativeId = "NOTKIEINITITATIVEID";
        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiativeId);

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId(initiativeId);

        // When
        EvaluationDTO result = ruleEngineService.applyRules(onboardingDTO, initiativeConfig);

        // Then
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, ((EvaluationCompletedDTO)result).getStatus());
        Assertions.assertEquals(Collections.emptyList(), ((EvaluationCompletedDTO)result).getOnboardingRejectionReasons());
    }

    @Test
    void testNotRules_kieBasedInitiatives(){
        // Given
        String initiativeId = "KIEINITITATIVEID_NOT_IN_CONTAINER";
        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiativeId);

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId(initiativeId);
        initiativeConfig.setAutomatedCriteria(List.of(AutomatedCriteriaDTO.builder().build()));

        Mockito.when(onboardingContextHolderServiceMock.getBeneficiaryRulesKieInitiativeIds())
                .thenReturn(Collections.emptySet());

        // When
        EvaluationDTO result = ruleEngineService.applyRules(onboardingDTO, initiativeConfig);

        // Then
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, ((EvaluationCompletedDTO)result).getStatus());
        Assertions.assertEquals(List.of(new OnboardingRejectionReason(
                        OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR,
                        OnboardingConstants.REJECTION_REASON_RULE_ENGINE_NOT_READY,
                        null, null, null
                )),
                ((EvaluationCompletedDTO)result).getOnboardingRejectionReasons());
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

    @Test
    void testEmptyDroolsContainer_noRules_nullCriteria() {
        testEmptyDroolsContainer("package dummy;", null);
    }

    @Test
    void testEmptyDroolsContainer_noAgendaRules_emptyCriteria() {
        testEmptyDroolsContainer("""
                package dummy;
                rule "RULENAME"
                when eval(true)
                then return;
                end
                """, Collections.emptyList());
    }

    void testEmptyDroolsContainer(String rule, List<AutomatedCriteriaDTO> automatedCriteriaDTOS) {
        // Given
        String initiativeId = "NOTKIEINITITATIVEID";

        KieBase emptyContainer = new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class))
                .build(Flux.just(DroolsRule.builder().
                        id(initiativeId)
                        .name("RULENAME")
                        .rule(rule)
                        .build())).block();
        Mockito.lenient() //not more invoked after fix
                .when(onboardingContextHolderServiceMock.getBeneficiaryRulesKieBase())
                .thenReturn(emptyContainer);

        OnboardingDTO onboardingDTO = new OnboardingDTO();
        onboardingDTO.setInitiativeId(initiativeId);

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId(initiativeId);
        initiativeConfig.setAutomatedCriteria(automatedCriteriaDTOS);

        // When
        EvaluationDTO result = ruleEngineService.applyRules(onboardingDTO, initiativeConfig);

        // Then
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, ((EvaluationCompletedDTO) result).getStatus());
        Assertions.assertEquals(Collections.emptyList(), ((EvaluationCompletedDTO) result).getOnboardingRejectionReasons());
    }
}
