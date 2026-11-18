package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.rest.onboarding.OnboardingRestClient;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesReqDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResDTO;
import it.gov.pagopa.admissibility.dto.onboarding.CheckPastOnboardingsSeriesResElemDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyPastOnboardingCheckServiceImplTest {

    private static final String INITIATIVE_ID = "initiative-1";
    private static final String USER_ID = "user-1";
    private static final String FAMILY_MEMBER_ID = "user-2";

    @Mock
    private OnboardingRestClient onboardingRestClient;

    private FamilyPastOnboardingCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FamilyPastOnboardingCheckServiceImpl(
                onboardingRestClient,
                new Onboarding2EvaluationMapper());
    }

    @Test
    void checkIsSkippedWhenInitiativeIsNullOrDisabled() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID));

        assertTrue(service.checkFamilyPastOnboardings(request, null).blockOptional().isEmpty());
        assertTrue(service.checkFamilyPastOnboardings(request, initiativeConfig(
                InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, true)).blockOptional().isEmpty());
        assertTrue(service.checkFamilyPastOnboardings(request, initiativeConfig(
                InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, false)).blockOptional().isEmpty());

        verifyNoInteractions(onboardingRestClient);
    }

    @Test
    void checkIsSkippedWhenThereAreNoOtherFamilyMembers() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID));

        assertTrue(service.checkFamilyPastOnboardings(request, initiativeConfig(
                InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true)).blockOptional().isEmpty());

        verifyNoInteractions(onboardingRestClient);
    }

    @Test
    void checkReturnsEmptyWhenNoFamilyMemberUsedPastBonus() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID, "user-3"));
        when(onboardingRestClient.checkPastOnboardingsSeries(
                eq(INITIATIVE_ID), any(CheckPastOnboardingsSeriesReqDTO.class)))
                .thenReturn(Mono.just(response(
                        result("user-3", false),
                        result(FAMILY_MEMBER_ID, false))));

        assertTrue(service.checkFamilyPastOnboardings(request, initiativeConfig(
                InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true)).blockOptional().isEmpty());

        ArgumentCaptor<CheckPastOnboardingsSeriesReqDTO> requestCaptor =
                ArgumentCaptor.forClass(CheckPastOnboardingsSeriesReqDTO.class);
        verify(onboardingRestClient).checkPastOnboardingsSeries(
                eq(INITIATIVE_ID), requestCaptor.capture());
        assertEquals(Set.of(FAMILY_MEMBER_ID, "user-3"),
                Set.copyOf(requestCaptor.getValue().getUserIds()));
    }

    @Test
    void checkReturnsRejectionWhenFamilyMemberUsedPastBonus() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID));
        when(onboardingRestClient.checkPastOnboardingsSeries(
                INITIATIVE_ID,
                new CheckPastOnboardingsSeriesReqDTO(List.of(FAMILY_MEMBER_ID))))
                .thenReturn(Mono.just(response(result(FAMILY_MEMBER_ID, true))));

        EvaluationCompletedDTO evaluation = assertInstanceOf(
                EvaluationCompletedDTO.class,
                service.checkFamilyPastOnboardings(request, initiativeConfig(
                        InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true)).block());

        assertNotNull(evaluation);
        assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluation.getStatus());
        OnboardingRejectionReason rejectionReason = evaluation.getOnboardingRejectionReasons().getFirst();
        assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_BONUS_ALREADY_USED,
                rejectionReason.getType());
        assertEquals(OnboardingConstants.REJECTION_REASON_FAMILY_BONUS_ALREADY_USED,
                rejectionReason.getCode());
    }

    @Test
    void checkErrorsWhenResponseIdsDoNotMatchRequest() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID));
        when(onboardingRestClient.checkPastOnboardingsSeries(
                INITIATIVE_ID,
                new CheckPastOnboardingsSeriesReqDTO(List.of(FAMILY_MEMBER_ID))))
                .thenReturn(Mono.just(response(result("unexpected-user", false))));

        Mono<EvaluationDTO> evaluation = service.checkFamilyPastOnboardings(
                request, initiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true));

        assertThrows(IllegalStateException.class, evaluation::block);
    }

    @Test
    void checkErrorsWhenResponseContainsDuplicateUserIds() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID));
        when(onboardingRestClient.checkPastOnboardingsSeries(
                INITIATIVE_ID,
                new CheckPastOnboardingsSeriesReqDTO(List.of(FAMILY_MEMBER_ID))))
                .thenReturn(Mono.just(response(
                        result(FAMILY_MEMBER_ID, false),
                        result(FAMILY_MEMBER_ID, false))));

        Mono<EvaluationDTO> evaluation = service.checkFamilyPastOnboardings(
                request, initiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true));

        assertThrows(IllegalStateException.class, evaluation::block);
    }

    @Test
    void checkPropagatesRestClientError() {
        OnboardingDTO request = requestWithFamily(Set.of(USER_ID, FAMILY_MEMBER_ID));
        RuntimeException failure = new RuntimeException("onboarding service unavailable");
        when(onboardingRestClient.checkPastOnboardingsSeries(
                INITIATIVE_ID,
                new CheckPastOnboardingsSeriesReqDTO(List.of(FAMILY_MEMBER_ID))))
                .thenReturn(Mono.error(failure));

        Mono<EvaluationDTO> evaluation = service.checkFamilyPastOnboardings(
                request, initiativeConfig(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, true));

        RuntimeException actual = assertThrows(RuntimeException.class, evaluation::block);

        assertEquals(failure, actual);
    }

    private static OnboardingDTO requestWithFamily(Set<String> memberIds) {
        return OnboardingDTO.builder()
                .userId(USER_ID)
                .initiativeId(INITIATIVE_ID)
                .family(new Family("family-1", memberIds))
                .build();
    }

    private static InitiativeConfig initiativeConfig(
            InitiativeGeneralDTO.BeneficiaryTypeEnum beneficiaryType,
            boolean checkPastOnboardingsSeries) {
        return InitiativeConfig.builder()
                .beneficiaryType(beneficiaryType)
                .checkPastOnboardingsSeries(checkPastOnboardingsSeries)
                .build();
    }

    private static CheckPastOnboardingsSeriesResDTO response(CheckPastOnboardingsSeriesResElemDTO... results) {
        return new CheckPastOnboardingsSeriesResDTO(List.of(results));
    }

    private static CheckPastOnboardingsSeriesResElemDTO result(String userId, boolean onboarded) {
        return new CheckPastOnboardingsSeriesResElemDTO(userId, onboarded);
    }
}
