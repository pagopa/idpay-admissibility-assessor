package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.repository.onboarding.OnboardingRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import it.gov.pagopa.admissibility.model.onboarding.OnboardingStatusInfo;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class FamilyPastOnboardingCheckServiceImplTest {

    private static final String INITIATIVE_ID = "INITIATIVEID";
    private static final String MEMBER_1 = "MEMBER_1";
    private static final String MEMBER_2 = "MEMBER_2";

    @Mock private OnboardingRepository onboardingRepositoryMock;

    private FamilyPastOnboardingCheckService service;

    @BeforeEach
    void init() {
        service = new FamilyPastOnboardingCheckServiceImpl(onboardingRepositoryMock, new Onboarding2EvaluationMapper());
    }

    @AfterEach
    void verifyNoMoreMockInvocations() {
        Mockito.verifyNoMoreInteractions(onboardingRepositoryMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {OnboardingConstants.DETAIL_KO_USER_BONUS_ALREADY_USED, OnboardingConstants.DETAIL_KO_FAMILY_BONUS_ALREADY_USED, "OTHER_REASON, USER_BONUS_ALREADY_USED"})
    void givenMemberWithBonusAlreadyUsedThenOnboardingKo(String detailKO) {
        OnboardingDTO request = buildRequest();
        InitiativeConfig initiativeConfig = buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true);

        Mockito.when(onboardingRepositoryMock.findByIdIn(expectedOnboardingIds()))
                .thenReturn(Flux.just(
                        statusInfo(MEMBER_1, OnboardingEvaluationStatus.ONBOARDING_OK.name(), null),
                        statusInfo(MEMBER_2, OnboardingEvaluationStatus.ONBOARDING_KO.name(), detailKO)));

        EvaluationDTO result = service.checkFamilyPastOnboardings(request, initiativeConfig).block();

        EvaluationCompletedDTO evaluation = Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluation.getStatus());
        Assertions.assertEquals(request.getUserId(), evaluation.getUserId());
        Assertions.assertEquals(1, evaluation.getOnboardingRejectionReasons().size());
        OnboardingRejectionReason reason = evaluation.getOnboardingRejectionReasons().getFirst();
        Assertions.assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_BONUS_ALREADY_USED, reason.getType());
        Assertions.assertEquals(OnboardingConstants.REJECTION_REASON_FAMILY_BONUS_ALREADY_USED, reason.getCode());
    }

    @Test
    void givenMembersWithoutBonusAlreadyUsedThenEmpty() {
        OnboardingDTO request = buildRequest();
        InitiativeConfig initiativeConfig = buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true);

        Mockito.when(onboardingRepositoryMock.findByIdIn(expectedOnboardingIds()))
                .thenReturn(Flux.just(
                        statusInfo(MEMBER_1, OnboardingEvaluationStatus.ONBOARDING_KO.name(), OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO),
                        statusInfo(MEMBER_2, OnboardingEvaluationStatus.ONBOARDING_OK.name(), OnboardingConstants.DETAIL_KO_USER_BONUS_ALREADY_USED)));

        Assertions.assertNull(service.checkFamilyPastOnboardings(request, initiativeConfig).block());
    }

    @Test
    void givenMemberKoWithoutDetailThenEmpty() {
        OnboardingDTO request = buildRequest();
        InitiativeConfig initiativeConfig = buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true);

        Mockito.when(onboardingRepositoryMock.findByIdIn(expectedOnboardingIds()))
                .thenReturn(Flux.just(statusInfo(MEMBER_1, OnboardingEvaluationStatus.ONBOARDING_KO.name(), null)));

        Assertions.assertNull(service.checkFamilyPastOnboardings(request, initiativeConfig).block());
    }

    @Test
    void givenNoMemberOnboardingThenEmpty() {
        OnboardingDTO request = buildRequest();
        InitiativeConfig initiativeConfig = buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true);

        Mockito.when(onboardingRepositoryMock.findByIdIn(expectedOnboardingIds())).thenReturn(Flux.empty());

        Assertions.assertNull(service.checkFamilyPastOnboardings(request, initiativeConfig).block());
    }

    @Test
    void givenCheckNotEnabledThenSkipQuery() {
        OnboardingDTO request = buildRequest();

        Assertions.assertNull(service.checkFamilyPastOnboardings(request, buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, false)).block());
        Assertions.assertNull(service.checkFamilyPastOnboardings(request, buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, null)).block());
        Assertions.assertNull(service.checkFamilyPastOnboardings(request, buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, true)).block());
        Assertions.assertNull(service.checkFamilyPastOnboardings(request, null).block());
    }

    @Test
    void givenNoOtherFamilyMembersThenSkipQuery() {
        InitiativeConfig initiativeConfig = buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true);

        OnboardingDTO onlyRequester = buildRequest();
        onlyRequester.setFamily(new Family("FAMILYID", Set.of(onlyRequester.getUserId())));
        Assertions.assertNull(service.checkFamilyPastOnboardings(onlyRequester, initiativeConfig).block());

        OnboardingDTO withoutFamily = buildRequest();
        withoutFamily.setFamily(null);
        Assertions.assertNull(service.checkFamilyPastOnboardings(withoutFamily, initiativeConfig).block());

        OnboardingDTO withoutMembers = buildRequest();
        withoutMembers.setFamily(new Family("FAMILYID", null));
        Assertions.assertNull(service.checkFamilyPastOnboardings(withoutMembers, initiativeConfig).block());
    }

    private static OnboardingDTO buildRequest() {
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, INITIATIVE_ID);
        request.setFamily(new Family("FAMILYID", Set.of(request.getUserId(), MEMBER_1, MEMBER_2)));
        return request;
    }

    private static Set<String> expectedOnboardingIds() {
        return Set.of(Onboarding.buildId(INITIATIVE_ID, MEMBER_1), Onboarding.buildId(INITIATIVE_ID, MEMBER_2));
    }

    private static InitiativeConfig buildInitiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum beneficiaryType, Boolean checkPastOnboardings) {
        return InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID)
                .beneficiaryType(beneficiaryType)
                .checkPastOnboardings(checkPastOnboardings)
                .build();
    }

    private static OnboardingStatusInfo statusInfo(String userId, String status, String detailKO) {
        return new OnboardingStatusInfo() {
            @Override public String getId() { return Onboarding.buildId(INITIATIVE_ID, userId); }
            @Override public String getStatus() { return status; }
            @Override public String getDetailKO() { return detailKO; }
        };
    }
}

