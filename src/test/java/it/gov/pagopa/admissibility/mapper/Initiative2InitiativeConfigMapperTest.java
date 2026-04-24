package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.CONSENT_CRITERIA_CODE_ISEE;

class Initiative2InitiativeConfigMapperTest {

    private final Initiative2InitiativeConfigMapper mapper =
            new Initiative2InitiativeConfigMapper();

    @Test
    void testAllField_fixedBudget() {

        Initiative2BuildDTO dto = initDto();
        dto.getGeneral().setRankingEnabled(true);
        setAdditionalInfo(dto);

        InitiativeConfig result = mapper.apply(dto);

        Assertions.assertNotNull(result);
        commonAssertions(dto, result);

        Assertions.assertEquals(
                List.of("CODE1", "CODE2", "CODE3"),
                result.getAutomatedCriteriaCodes()
        );
        Assertions.assertTrue(result.isRankingInitiative());

        Assertions.assertEquals(
                List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build(),
                        Order.builder().fieldCode("CODE2").direction(Sort.Direction.DESC).build()
                ),
                result.getRankingFields()
        );

        Assertions.assertTrue(result.getIsLogoPresent());

        // budget fixed ⇒ max null
        Assertions.assertNull(result.getBeneficiaryBudgetMaxCents());

        TestUtils.checkNotNullFields(
                result,
                "beneficiaryBudgetMaxCents"
        );
    }

    @Test
    void testAdditionalInfoNull() {

        Initiative2BuildDTO dto = initDto();
        dto.getGeneral().setRankingEnabled(true);

        InitiativeConfig result = mapper.apply(dto);

        Assertions.assertNotNull(result);
        commonAssertions(dto, result);

        Assertions.assertFalse(result.getIsLogoPresent());
        Assertions.assertNull(result.getBeneficiaryBudgetMaxCents());

        TestUtils.checkNotNullFields(
                result,
                "beneficiaryBudgetMaxCents"
        );
    }

    @Test
    void testRankingFalse() {

        Initiative2BuildDTO dto = initDto();
        dto.getGeneral().setRankingEnabled(false);
        setAdditionalInfo(dto);
        dto.getAdditionalInfo().setLogoFileName("");

        InitiativeConfig result = mapper.apply(dto);

        Assertions.assertNotNull(result);
        commonAssertions(dto, result);

        Assertions.assertFalse(result.isRankingInitiative());
        Assertions.assertFalse(result.getIsLogoPresent());

        TestUtils.checkNotNullFields(
                result,
                "rankingFields",
                "beneficiaryBudgetMaxCents"
        );
    }

    @Test
    void testVariableBudgetWithoutAnyThreshold() {

        Initiative2BuildDTO dto = initDto();
        dto.getGeneral().setBeneficiaryBudgetFixedCents(null);

        dto.setBeneficiaryRule(
                InitiativeBeneficiaryRuleDTO.builder()
                        .selfDeclarationCriteria(
                                List.of(
                                        SelfCriteriaMultiConsentDTO.builder()
                                                .value(
                                                        List.of(
                                                                SelfCriteriaMultiConsentDTO.ConsentValue.builder()
                                                                        .code(CONSENT_CRITERIA_CODE_ISEE)
                                                                        .beneficiaryBudgetMaxCents(null)
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                        )
                        .build()
        );

        InitiativeConfig result = mapper.apply(dto);

        Assertions.assertNull(result.getBeneficiaryBudgetMaxCents());
    }

    private Initiative2BuildDTO initDto() {

        Initiative2BuildDTO dto = new Initiative2BuildDTO();
        dto.setInitiativeId("INITIATIVEID");
        dto.setInitiativeName("INITIATIVENAME");
        dto.setOrganizationId("ORGANIZATIONID");
        dto.setOrganizationName("ORGANIZATIONNAME");
        dto.setStatus("STATUS");
        dto.setInitiativeRewardType("REFUND");

        dto.setGeneral(
                InitiativeGeneralDTO.builder()
                        .startDate(LocalDate.MIN)
                        .endDate(LocalDate.MAX)
                        .budgetCents(1000_00L)
                        .beneficiaryBudgetFixedCents(1_00L)
                        .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                        .build()
        );

        dto.setBeneficiaryRule(
                InitiativeBeneficiaryRuleDTO.builder()
                        .automatedCriteria(
                                List.of(
                                        AutomatedCriteriaDTO.builder()
                                                .code("CODE1")
                                                .orderDirection(Sort.Direction.ASC)
                                                .build(),
                                        AutomatedCriteriaDTO.builder()
                                                .code("CODE2")
                                                .orderDirection(Sort.Direction.DESC)
                                                .build(),
                                        AutomatedCriteriaDTO.builder()
                                                .code("CODE3")
                                                .build()
                                )
                        )
                        .selfDeclarationCriteria(
                                List.of(
                                        buildSelfCriteriaMultiConsent(
                                                CONSENT_CRITERIA_CODE_ISEE,
                                                "THRESHOLD",
                                                10_00L
                                        )
                                )
                        )
                        .build()
        );

        return dto;
    }

    private void setAdditionalInfo(Initiative2BuildDTO dto) {
        dto.setAdditionalInfo(
                InitiativeAdditionalInfoDTO.builder()
                        .serviceName("SERVICE")
                        .logoFileName("logo.png")
                        .build()
        );
    }

    private void commonAssertions(Initiative2BuildDTO dto, InitiativeConfig result) {
        Assertions.assertEquals(dto.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(dto.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(dto.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(dto.getOrganizationName(), result.getOrganizationName());
        Assertions.assertEquals(dto.getStatus(), result.getStatus());
        Assertions.assertEquals(
                dto.getGeneral().getBudgetCents(),
                result.getInitiativeBudgetCents()
        );
        Assertions.assertEquals(
                dto.getGeneral().getBeneficiaryBudgetFixedCents(),
                result.getBeneficiaryBudgetFixedCents()
        );
        Assertions.assertEquals(
                dto.getGeneral().getBeneficiaryType(),
                result.getBeneficiaryType()
        );
    }

    private SelfCriteriaMultiConsentDTO buildSelfCriteriaMultiConsent(
            String consentCode,
            String thresholdCode,
            Long beneficiaryBudgetMaxCents
    ) {
        return SelfCriteriaMultiConsentDTO.builder()
                .value(
                        List.of(
                                SelfCriteriaMultiConsentDTO.ConsentValue.builder()
                                        .code(consentCode)
                                        .thresholdCode(thresholdCode)
                                        .beneficiaryBudgetMaxCents(beneficiaryBudgetMaxCents)
                                        .build()
                        )
                )
                .build();
    }
}