package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerImplTest;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public class BeneficiaryRule2DroolsRuleImplTest {

    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final CriteriaCodeService criteriaCodeServiceMock;

    public BeneficiaryRule2DroolsRuleImplTest() {
        this.criteriaCodeServiceMock = Mockito.mock(CriteriaCodeService.class);
        this.beneficiaryRule2DroolsRule = new BeneficiaryRule2DroolsRuleImpl(criteriaCodeServiceMock, ExtraFilter2DroolsTransformerImplTest.extraFilter2DroolsTransformer);
    }

    @BeforeEach
    public void configurMock(){
        configureCriteriaMock("CODE1", "isee");
        configureCriteriaMock("CODE2", "criteriaConsensusTimestamp");
    }

    private void configureCriteriaMock(String code, String field){
        CriteriaCodeConfig codeConfig = new CriteriaCodeConfig();
        codeConfig.setOnboardingField(field);
        Mockito.when(criteriaCodeServiceMock.getCriteriaCodeConfig(code)).thenReturn(codeConfig);
    }

    @Test
    public void testBuild(){
        // given
        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("ID");
        dto.setInitiativeName("NAME");
        dto.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        List<AutomatedCriteriaDTO> criterias = new ArrayList<>();

        criterias.add(new AutomatedCriteriaDTO("AUTH1", "CODE1", null, FilterOperator.EQ, "1"));
        criterias.add(new AutomatedCriteriaDTO("AUTH2", "CODE2", "dayOfWeek", FilterOperator.GT, "MONDAY"));

        dto.getBeneficiaryRule().setAutomatedCriteria(criterias);

        // when
        DroolsRule result = beneficiaryRule2DroolsRule.apply(Flux.just(dto)).blockFirst();

        // then
        DroolsRule expected = new DroolsRule();
        expected.setName("ID - NAME");
        expected.setAgendaGroup("ID");
        expected.setRuleCondition("$rejectionReasons: new java.util.ArrayList();\n" +
                "$onboarding: it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO();\n" +
                "eval(isee == new java.math.BigDecimal(\"1\") ? true : $rejectionReasons.add(\"AUTOMATED_CRITERIA_CODE1_FAIL\"))\n" +
                "eval(criteriaConsensusTimestamp.dayOfWeek > java.time.DayOfWeek.valueOf(\"MONDAY\") ? true : $rejectionReasons.add(\"AUTOMATED_CRITERIA_CODE2_FAIL\"))\n" +
                "$rejectionReason.size()>0");
        expected.setRuleConsequence("$onboarding.getOnboardingRejectionReasons().addAll($rejectionReasons)");

        Assertions.assertEquals(expected, result);
    }
}
