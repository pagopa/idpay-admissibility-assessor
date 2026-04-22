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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

class Onboarding2EvaluationMapperTest {

    private Onboarding2EvaluationMapper mapper;
    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setUp() {
        mapper = new Onboarding2EvaluationMapper();

        LocalDateTime now = LocalDateTime.now();

        onboardingRequest = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("OK")
                .pdndAccept(true)
                .tcAcceptTimestamp(now)
                .criteriaConsensusTimestamp(now)
                .isee(new BigDecimal(100))
                .residence(new Residence())
                .birthDate(new BirthDate())
                .family(Family.builder()
                        .familyId("FAMILYID")
                        .memberIds(Set.of("USERID"))
                        .build())
                .serviceId("SERVICE")
                .verifies(new ArrayList<>())
                .userMail("USERMAIL")
                .channel("CHANNEL")
                .name("NAME")
                .surname("SURNAME")
                .build();

        initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");
        initiativeConfig.setOrganizationName("ORGANIZATIONNAME");
        initiativeConfig.setBeneficiaryBudgetFixedCents(10_00L);
        initiativeConfig.setInitiativeRewardType("REFUND");
        initiativeConfig.setIsLogoPresent(Boolean.FALSE);
        initiativeConfig.setEndDate(LocalDate.now());
    }

    @Test
    void onboardingOk_noBlockingVerifyKo() {
        initiativeConfig.setRankingInitiative(false);

        onboardingRequest.getVerifies().add(
                blockingVerify(true)
        );

        EvaluationDTO result =
                mapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);

        EvaluationCompletedDTO completed = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, completed.getStatus());
        Assertions.assertEquals(initiativeConfig.getBeneficiaryBudgetFixedCents(),
                completed.getBeneficiaryBudgetCents());
        Assertions.assertTrue(CollectionUtils.isEmpty(completed.getOnboardingRejectionReasons()));
    }

    @Test
    void rankingRequest_ok() {
        configureRanking();

        onboardingRequest.getVerifies().add(blockingVerify(true));

        EvaluationDTO result =
                mapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        Assertions.assertInstanceOf(RankingRequestDTO.class, result);

        RankingRequestDTO ranking = (RankingRequestDTO) result;
        Assertions.assertEquals(
                CommonUtilities.euroToCents(onboardingRequest.getIsee()),
                ranking.getRankingValue()
        );
    }

    @Test
    void evaluationCompletedToRankingRequest() {
        configureRanking();

        onboardingRequest.getVerifies().add(blockingVerify(true));

        EvaluationDTO evaluation =
                mapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());

        Assertions.assertInstanceOf(RankingRequestDTO.class, evaluation);
    }

    @Test
    void rankingRequest_rejected_setsOnboardingKoTrue() {

        EvaluationCompletedDTO completed = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .status(OnboardingEvaluationStatus.REJECTED)
                .build();

        RankingRequestDTO result =
                mapper.apply(onboardingRequest, completed);

        Assertions.assertTrue(result.isOnboardingKo());
    }

    @Test
    void rankingRequest_ok_setsOnboardingKoFalse() {

        EvaluationCompletedDTO completed = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .build();

        RankingRequestDTO result =
                mapper.apply(onboardingRequest, completed);

        Assertions.assertFalse(result.isOnboardingKo());
    }
    @Test
    void rankingRequest_withoutFamily_doesNotSetFamilyFields() {

        onboardingRequest.setFamily(null);

        EvaluationCompletedDTO completed = EvaluationCompletedDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVE")
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .build();

        RankingRequestDTO result =
                mapper.apply(onboardingRequest, completed);

        Assertions.assertNull(result.getFamilyId());
        Assertions.assertNull(result.getMemberIds());
    }



    private VerifyDTO blockingVerify(boolean result) {
        return new VerifyDTO(
                OnboardingConstants.CRITERIA_CODE_ISEE,
                true,
                true, // bloccante
                null,
                null,
                null,
                result
        );
    }

    private void configureRanking() {
        initiativeConfig.setRankingInitiative(true);
        initiativeConfig.setRankingFields(List.of(
                Order.builder()
                        .fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE)
                        .direction(Sort.Direction.ASC)
                        .build()
        ));
    }
}
