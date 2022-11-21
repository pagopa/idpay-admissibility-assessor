package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

class Onboarding2EvaluationMapperTest {

    private Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setUp() {
        onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

        //init onboarding
        Map<String, Boolean> selfDeclarationList = new HashMap<>();
        selfDeclarationList.put("MAP", true);

        LocalDateTime acceptanceDateTime = LocalDateTime.now();

        onboardingRequest = new OnboardingDTO(
                "USERID",
                "INITIATIVEID",
                true,
                "OK",
                true,
                selfDeclarationList,
                acceptanceDateTime,
                acceptanceDateTime,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        //init initiativeConfig
        initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");
        initiativeConfig.setBeneficiaryInitiativeBudget(BigDecimal.TEN);
        initiativeConfig.setServiceId("SERVICEID");
        initiativeConfig.setRankingInitiative(Boolean.FALSE);

        LocalDate endDate = LocalDate.now();
        initiativeConfig.setEndDate(endDate);
    }

    @Test
    void onboarding2EvaluationOnboardingOkTest() {

        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = new ArrayList<>();
        initiativeConfig.setRankingInitiative(Boolean.FALSE);

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);

        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted);


        Assertions.assertEquals("ONBOARDING_OK", resultCompleted.getStatus());
        Assertions.assertEquals(0, BigDecimal.TEN.compareTo(resultCompleted.getBeneficiaryBudget()));
        Assertions.assertTrue(CollectionUtils.isEmpty(resultCompleted.getOnboardingRejectionReasons()));

        TestUtils.checkNotNullFields(resultCompleted);
    }

    @Test
    void onboarding2EvaluationOnboardingKoTest() {

        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, null, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals("ONBOARDING_KO", resultCompleted.getStatus());

        Assertions.assertNull(resultCompleted.getInitiativeName());
        Assertions.assertNull(resultCompleted.getOrganizationId());
        Assertions.assertNull(resultCompleted.getBeneficiaryBudget());
        Assertions.assertNull(resultCompleted.getInitiativeEndDate());

        Assertions.assertEquals(rejectReasons, resultCompleted.getOnboardingRejectionReasons());

        TestUtils.checkNotNullFields(resultCompleted, "initiativeName", "organizationId", "serviceId", "initiativeEndDate", "beneficiaryBudget");
    }

    @Test
    void onboarding2EvaluationOnboardingOkNullRejectedReasonTest() {
        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, null);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals("ONBOARDING_OK", resultCompleted.getStatus());

        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted);

        TestUtils.checkNotNullFields(resultCompleted,"onboardingRejectionReasons");
    }

    @Test
    void onboarding2EvaluationOnboardingOkRankingInitiativeTest() {

        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = new ArrayList<>();

        initiativeConfig.setRankingInitiative(Boolean.TRUE);
        initiativeConfig.setRankingFieldCodes(List.of(OnboardingConstants.CRITERIA_CODE_ISEE));

        LocalDate endDate = LocalDate.now();
        initiativeConfig.setEndDate(endDate);


        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        commonAssertionRankingRequestOk(result, false);
    }

    @Test
    void onboarding2EvaluationOnboardingOkRankingInitiativeEmptyRejectionReasonTest() {
        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = new ArrayList<>();

        initiativeConfig.setRankingInitiative(Boolean.TRUE);
        initiativeConfig.setRankingFieldCodes(List.of(OnboardingConstants.CRITERIA_CODE_RESIDENCE, OnboardingConstants.CRITERIA_CODE_ISEE));

        LocalDate endDate = LocalDate.now();
        initiativeConfig.setEndDate(endDate);


        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        commonAssertionRankingRequestOk(result, false);
    }

    @Test
    void onboarding2EvaluationOnboardingKoRankingTest() {
        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());

        initiativeConfig.setRankingInitiative(Boolean.TRUE);
        initiativeConfig.setRankingFieldCodes(List.of(OnboardingConstants.CRITERIA_CODE_ISEE));
        initiativeConfig.setEndDate(LocalDate.now());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof EvaluationCompletedDTO);

        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        System.out.println(resultCompleted);
        commonAssertionsOnboarding2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals("ONBOARDING_KO", resultCompleted.getStatus());


        commonAssertionsInitiativeConfig2EvaluationCompleted(resultCompleted);
        Assertions.assertEquals(initiativeConfig.getBeneficiaryInitiativeBudget(), resultCompleted.getBeneficiaryBudget());
        Assertions.assertNotNull(resultCompleted.getInitiativeEndDate());

        Assertions.assertEquals(rejectReasons, resultCompleted.getOnboardingRejectionReasons());

        TestUtils.checkNotNullFields(resultCompleted);
    }
    @Test
    void onboarding2EvaluationOnboardingOkRankingEmptyRejectionsTest() {
        // GIVEN
        List<OnboardingRejectionReason> rejectReasons = Collections.emptyList();

        initiativeConfig.setRankingInitiative(Boolean.TRUE);
        initiativeConfig.setRankingFieldCodes(List.of(OnboardingConstants.CRITERIA_CODE_ISEE));
        initiativeConfig.setEndDate(LocalDate.now());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        commonAssertionRankingRequestOk(result, true);
    }

    @Test
    void onboarding2EvaluationOnboardingOkRankingNullRejectionsTest() {
        // GIVEN
        initiativeConfig.setRankingInitiative(Boolean.TRUE);
        initiativeConfig.setRankingFieldCodes(List.of(OnboardingConstants.CRITERIA_CODE_ISEE));
        initiativeConfig.setEndDate(LocalDate.now());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, null);

        // THEN
        commonAssertionRankingRequestOk(result,true);
    }

    private void commonAssertionRankingRequestOk(EvaluationDTO result, boolean isRankingValue) {
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof RankingRequestDTO);

        RankingRequestDTO resultRankingRequest = (RankingRequestDTO) result;
        Assertions.assertEquals(onboardingRequest.getUserId(), resultRankingRequest.getUserId());
        Assertions.assertEquals(onboardingRequest.getInitiativeId(), resultRankingRequest.getInitiativeId());
        Assertions.assertEquals(onboardingRequest.getCriteriaConsensusTimestamp(), resultRankingRequest.getCriteriaConsensusTimestamp());
        if(isRankingValue){
            TestUtils.checkNotNullFields(resultRankingRequest);
        }
        else {
            TestUtils.checkNotNullFields(resultRankingRequest, "rankingValue");
        }
    }

    private void commonAssertionsInitiativeConfig2EvaluationCompleted(EvaluationCompletedDTO resultCompleted) {
        Assertions.assertEquals(initiativeConfig.getInitiativeName(),resultCompleted.getInitiativeName());
        Assertions.assertEquals(initiativeConfig.getInitiativeId(), resultCompleted.getInitiativeId());
        Assertions.assertEquals(initiativeConfig.getOrganizationId(), resultCompleted.getOrganizationId());
        Assertions.assertEquals(initiativeConfig.getEndDate(), resultCompleted.getInitiativeEndDate());
    }

    private void commonAssertionsOnboarding2EvaluationCompleted(EvaluationCompletedDTO resultCompleted) {
        Assertions.assertEquals(onboardingRequest.getUserId(), resultCompleted.getUserId());
        Assertions.assertEquals(onboardingRequest.getInitiativeId(), resultCompleted.getInitiativeId());
        Assertions.assertEquals(onboardingRequest.getCriteriaConsensusTimestamp(), resultCompleted.getCriteriaConsensusTimestamp());
    }
}
