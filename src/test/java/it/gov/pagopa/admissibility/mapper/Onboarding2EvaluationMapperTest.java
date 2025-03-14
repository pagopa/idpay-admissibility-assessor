package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Onboarding2EvaluationMapperTest {

    private Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setUp() {
        onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

        //init onboarding
        LocalDateTime acceptanceDateTime = LocalDateTime.now();

        onboardingRequest = new OnboardingDTO(
                "USERID",
                "INITIATIVEID",
                true,
                "OK",
                true,
                acceptanceDateTime,
                acceptanceDateTime,
                new BigDecimal(100),
                new Residence(),
                new BirthDate(),
                Family.builder()
                        .familyId("FAMILYID")
                        .memberIds(Set.of("USERID")).build(),
                false,
                "SERVICE"
        );

        //init initiativeConfig
        initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");
        initiativeConfig.setOrganizationName("ORGANIZATIONNAME");
        initiativeConfig.setBeneficiaryInitiativeBudgetCents(10_00L);
        initiativeConfig.setInitiativeRewardType("REFUND");
        initiativeConfig.setIsLogoPresent(Boolean.FALSE);
        initiativeConfig.setEndDate(LocalDate.now());
    }

    @Test
    void onboarding2EvaluationOnboardingOkTest() {

        // GIVEN
        initiativeConfig.setRankingInitiative(false);

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);

        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted, null);


        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, resultCompleted.getStatus());
        Assertions.assertEquals(0, Long.valueOf(10_00L).compareTo(resultCompleted.getBeneficiaryBudgetCents()));
        Assertions.assertTrue(CollectionUtils.isEmpty(resultCompleted.getOnboardingRejectionReasons()));

        TestUtils.checkNotNullFields(resultCompleted,"rankingValue");
    }

    @Test
    void onboarding2EvaluationOnboardingKoTest() {

        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());

        onboardingRequest.setFamily(null);

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, null, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, resultCompleted.getStatus());

        Assertions.assertNull(resultCompleted.getInitiativeName());
        Assertions.assertNull(resultCompleted.getOrganizationId());
        Assertions.assertNull(resultCompleted.getOrganizationName());
        Assertions.assertNull(resultCompleted.getBeneficiaryBudgetCents());
        Assertions.assertNull(resultCompleted.getInitiativeEndDate());
        Assertions.assertNull(resultCompleted.getInitiativeRewardType());
        Assertions.assertNull(resultCompleted.getRankingValue());
        Assertions.assertNull(resultCompleted.getIsLogoPresent());

        Assertions.assertEquals(rejectReasons, resultCompleted.getOnboardingRejectionReasons());

        TestUtils.checkNotNullFields(resultCompleted, "familyId", "memberIds", "initiativeName", "organizationId", "organizationName", "serviceId", "initiativeEndDate", "beneficiaryBudgetCents", "rankingValue", "initiativeRewardType", "isLogoPresent");
    }

    @Test
    void onboarding2EvaluationOnboardingKoWithInitiativeConfigTest() {

        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());
        initiativeConfig.setRankingInitiative(false);

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, resultCompleted.getStatus());

        Assertions.assertEquals(rejectReasons, resultCompleted.getOnboardingRejectionReasons());

        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted, null);

        TestUtils.checkNotNullFields(resultCompleted,"rankingValue");
    }

    @Test
    void onboarding2EvaluationOnboardingOkRankingEmptyRejectionReasonFirstRankingCodeNotISEETest() {
        // GIVEN
        initiativeConfig.setRankingInitiative(true);
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.DESC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        initiativeConfig.setEndDate(LocalDate.now());


        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        // THEN
        commonAssertionRankingRequestOk(result, -1L);
    }

    @Test
    void onboarding2EvaluationOnboardingKoRankingTest() {
        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());
        configureRankingInitiative();
        initiativeConfig.setEndDate(LocalDate.now());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        System.out.println(resultCompleted);
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, resultCompleted.getStatus());

        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted, CommonUtilities.euroToCents(onboardingRequest.getIsee()));
        Assertions.assertEquals(initiativeConfig.getBeneficiaryInitiativeBudgetCents(), resultCompleted.getBeneficiaryBudgetCents());
        Assertions.assertNotNull(resultCompleted.getInitiativeEndDate());

        Assertions.assertEquals(rejectReasons, resultCompleted.getOnboardingRejectionReasons());

        TestUtils.checkNotNullFields(resultCompleted);
    }

    @Test
    void onboarding2EvaluationOnboardingOkRankingEmptyRejectionReasonTest() {
        // GIVEN
        configureRankingInitiative();
        initiativeConfig.setEndDate(LocalDate.now());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        // THEN
        commonAssertionRankingRequestOk(result, CommonUtilities.euroToCents(onboardingRequest.getIsee()));
    }

    private void configureRankingInitiative() {
        initiativeConfig.setRankingInitiative(true);
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));
    }

    private void commonAssertionRankingRequestOk(EvaluationDTO result, long expectedRankingValue) {
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof RankingRequestDTO);

        RankingRequestDTO resultRankingRequest = (RankingRequestDTO) result;
        Assertions.assertEquals(onboardingRequest.getUserId(), resultRankingRequest.getUserId());
        Assertions.assertEquals(onboardingRequest.getInitiativeId(), resultRankingRequest.getInitiativeId());
        Assertions.assertEquals(initiativeConfig.getOrganizationId(), resultRankingRequest.getOrganizationId());
        Assertions.assertEquals(onboardingRequest.getCriteriaConsensusTimestamp(), resultRankingRequest.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(expectedRankingValue, resultRankingRequest.getRankingValue());

        TestUtils.checkNotNullFields(resultRankingRequest);
    }

    private void commonAssertionsInitiativeConfig2EvaluationCompleted(EvaluationCompletedDTO resultCompleted, Long expectedRankingValue) {
        Assertions.assertEquals(initiativeConfig.getInitiativeName(), resultCompleted.getInitiativeName());
        Assertions.assertEquals(initiativeConfig.getInitiativeId(), resultCompleted.getInitiativeId());
        Assertions.assertEquals(initiativeConfig.getOrganizationId(), resultCompleted.getOrganizationId());
        Assertions.assertEquals(initiativeConfig.getOrganizationName(), resultCompleted.getOrganizationName());
        Assertions.assertEquals(initiativeConfig.getEndDate(), resultCompleted.getInitiativeEndDate());
        Assertions.assertEquals(initiativeConfig.getInitiativeRewardType(), resultCompleted.getInitiativeRewardType());
        Assertions.assertEquals(initiativeConfig.getIsLogoPresent(), resultCompleted.getIsLogoPresent());

        Assertions.assertEquals(expectedRankingValue, resultCompleted.getRankingValue());
    }

    private void commonAssertionsOnboarding2EvaluationCompleted(EvaluationCompletedDTO resultCompleted) {
        Assertions.assertEquals(onboardingRequest.getUserId(), resultCompleted.getUserId());
        Assertions.assertEquals(onboardingRequest.getFamily()!=null?onboardingRequest.getFamily().getFamilyId() : null, resultCompleted.getFamilyId());
        Assertions.assertEquals(onboardingRequest.getInitiativeId(), resultCompleted.getInitiativeId());
        Assertions.assertEquals(onboardingRequest.getCriteriaConsensusTimestamp(), resultCompleted.getCriteriaConsensusTimestamp());
    }

    @Test
    void evaluationCompleted2RankingRequest_onboardingKo() {
        evaluationCompleted2RankingRequest(true);
    }
    @Test
    void evaluationCompleted2RankingRequest_onboardingOk() {
        evaluationCompleted2RankingRequest(false);
    }
    void evaluationCompleted2RankingRequest(boolean onboardingKo) {
        configureRankingInitiative();

        EvaluationDTO evaluationDTO = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, List.of(new OnboardingRejectionReason()));

        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, evaluationDTO);

        EvaluationCompletedDTO evaluationCompletedDTO = (EvaluationCompletedDTO) evaluationDTO;
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluationCompletedDTO.getStatus());

        if(!onboardingKo){
            evaluationCompletedDTO.setStatus(OnboardingEvaluationStatus.ONBOARDING_OK);
        }

        RankingRequestDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, evaluationCompletedDTO);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(evaluationCompletedDTO.getUserId(), result.getUserId());
        Assertions.assertEquals(evaluationCompletedDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(evaluationCompletedDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(evaluationCompletedDTO.getAdmissibilityCheckDate(), result.getAdmissibilityCheckDate());
        Assertions.assertEquals(evaluationCompletedDTO.getCriteriaConsensusTimestamp(), result.getCriteriaConsensusTimestamp());
        Assertions.assertEquals(Optional.ofNullable(evaluationCompletedDTO.getRankingValue()).orElse(-1L), result.getRankingValue());
        Assertions.assertEquals(onboardingKo, result.isOnboardingKo());

        commonAssertionRankingRequestOk(result, CommonUtilities.euroToCents(onboardingRequest.getIsee()));
    }
}
