package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class Evaluation2RankingRequestMapperTest {
    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private EvaluationDTO evaluationDTO;
    private Evaluation2RankingRequestMapper evaluation2RankingRequestMapper;

    @BeforeEach
    void setUp() {
        evaluation2RankingRequestMapper = new Evaluation2RankingRequestMapper();

        LocalDateTime localDateTimeNow= LocalDateTime.now();
        LocalDate LocalDateNow = LocalDate.from(localDateTimeNow).plusMonths(2L);

        onboardingDTO = OnboardingDTOFaker.mockInstanceBuilder(1,1)
                .isee(BigDecimal.ONE)
                .build();


        initiativeConfig = InitiativeConfig.builder()
                .initiativeId(onboardingDTO.getInitiativeId())
                .initiativeName("INITIATIVE_NAME")
                .endDate(LocalDateNow)
                .organizationId("ORGANIZATION_ID")
                .serviceId("SERVICE_ID")
                .build();

        evaluationDTO = EvaluationDTO.builder()
                .userId(onboardingDTO.getUserId())
                .initiativeId(onboardingDTO.getInitiativeId())
                .initiativeName(initiativeConfig.getInitiativeName())
                .initiativeEndDate(LocalDateNow)
                .organizationId(initiativeConfig.getOrganizationId())
                .status("STATUS")
                .admissibilityCheckDate(localDateTimeNow)
                .onboardingRejectionReasons(List.of(
                        OnboardingRejectionReason.builder().code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED).build()))
                .beneficiaryBudget(BigDecimal.TEN)
                .serviceId(initiativeConfig.getServiceId())
                .criteriaConsensusTimestamp(localDateTimeNow)
                .build();
    }

    @Test
    void applyWithIseeInRankingFieldCodes() {
        // Given
        initiativeConfig.setRankingFieldCodes(List.of("ISEE"));

        // When
        RankingRequestDTO result = evaluation2RankingRequestMapper.apply(ImmutableTriple.of(evaluationDTO, initiativeConfig, onboardingDTO));

        // Then
        Assertions.assertNotNull(result);

        commonAssertions(result);
        Assertions.assertEquals(onboardingDTO.getIsee(), result.getRankingValue());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void applyWithoutIseeInRankingFieldCodes() {
        // Given
        initiativeConfig.setRankingFieldCodes(List.of("ANOTHER_CODE"));

        // When
        RankingRequestDTO result = evaluation2RankingRequestMapper.apply(ImmutableTriple.of(evaluationDTO, initiativeConfig, onboardingDTO));

        // Then
        Assertions.assertNotNull(result);

        commonAssertions(result);

        TestUtils.checkNotNullFields(result,"rankingValue");
    }

    private void commonAssertions(RankingRequestDTO result) {
        Assertions.assertEquals(evaluationDTO.getUserId(), result.getUserId());
        Assertions.assertEquals(evaluationDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(evaluationDTO.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(evaluationDTO.getInitiativeEndDate(), result.getInitiativeEndDate());
        Assertions.assertEquals(evaluationDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(evaluationDTO.getAdmissibilityCheckDate(), result.getAdmissibilityCheckDate());
        Assertions.assertEquals(evaluationDTO.getOnboardingRejectionReasons(), result.getOnboardingRejectionReasons());
        Assertions.assertEquals(evaluationDTO.getBeneficiaryBudget(), result.getBeneficiaryBudget());
        Assertions.assertEquals(evaluationDTO.getServiceId(), result.getServiceId());
        Assertions.assertEquals(evaluationDTO.getCriteriaConsensusTimestamp(), result.getCriteriaConsensusTimestamp());
    }
}