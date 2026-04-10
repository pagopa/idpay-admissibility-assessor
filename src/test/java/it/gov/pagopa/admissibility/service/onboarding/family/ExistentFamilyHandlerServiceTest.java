package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.exception.FamilyAlreadyOnBoardingException;
import it.gov.pagopa.admissibility.exception.SkipAlreadyRankingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ExistentFamilyHandlerServiceTest {

    @Mock private OnboardingRescheduleService onboardingRescheduleServiceMock;

    private final Onboarding2EvaluationMapper mapper = new Onboarding2EvaluationMapper();
    private final Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = new Onboarding2OnboardingDroolsMapper();
    private ExistentFamilyHandlerService service;

    @BeforeEach
    void init(){

        service = new ExistentFamilyHandlerServiceImpl(1, new Onboarding2EvaluationMapper(), onboardingRescheduleServiceMock, onboarding2OnboardingDroolsMapper);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(onboardingRescheduleServiceMock);
    }


    private final OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
    private final OnboardingFamilies family = OnboardingFamilies.builder(new Family("FAMILYID", Set.of(request.getUserId())), request.getInitiativeId())
            .build();
    private final InitiativeConfig initiativeConfig = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .build();
    private final Message<String> message = MessageBuilder.withPayload("").build();

    @Test
    void testInProgress(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.IN_PROGRESS);
        Instant requestDateTime = Instant.now();

        // When
        Mono<EvaluationDTO> mono = service.handleExistentFamily(request, family, initiativeConfig, message);
        Assertions.assertThrows(WaitingFamilyOnBoardingException.class, mono::block);

        Assertions.assertEquals(new Family(family.getFamilyId(), family.getMemberIds()), request.getFamily());

        Mockito.verify(onboardingRescheduleServiceMock, Mockito.never()).reschedule(
                Mockito.same(request),
                Mockito.argThat(dt -> dt.isAfter(requestDateTime) && dt.isBefore(requestDateTime.plus(2, ChronoUnit.MINUTES))),
                Mockito.eq("Family FAMILYID onboarding IN_PROGRESS into initiative INITIATIVEID"),
                Mockito.same(message));
    }

    @Test
    void testInProgressByUserResubmitted(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.IN_PROGRESS);
        family.setCreateBy(request.getUserId());

        // When
        EvaluationDTO result = service.handleExistentFamily(request, family, initiativeConfig, message).block();

        Assertions.assertNull(result);
        Mockito.verifyNoMoreInteractions(onboardingRescheduleServiceMock);
    }

    @Test
    void testOnboardingOk(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.ONBOARDING_OK);

        // When
        testCompletedFamilyOnboarding(OnboardingEvaluationStatus.JOINED);
    }

    @Test
    void testOnboardingKo(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.ONBOARDING_KO);
        family.setOnboardingRejectionReasons(List.of(new OnboardingRejectionReason()));

        testCompletedFamilyOnboarding(OnboardingEvaluationStatus.REJECTED);
    }

    private void testCompletedFamilyOnboarding(OnboardingEvaluationStatus expectedStatus) {
        OnboardingDroolsDTO onboardingDroolsDTO = onboarding2OnboardingDroolsMapper.apply(request);
        //Given
        EvaluationCompletedDTO expectedResult = (EvaluationCompletedDTO) mapper.apply(onboardingDroolsDTO, initiativeConfig, family.getOnboardingRejectionReasons());

        // When
        EvaluationDTO result = service.handleExistentFamily(request, family, initiativeConfig, message).block();

        // Then
        expectedResult.setFamilyId(family.getFamilyId());
        expectedResult.setMemberIds(family.getMemberIds());
        expectedResult.setStatus(expectedStatus);

        Assertions.assertNotNull(result);
        // the mapper use now()
        Assertions.assertFalse(expectedResult.getAdmissibilityCheckDate().isAfter(result.getAdmissibilityCheckDate()));
        expectedResult.setAdmissibilityCheckDate(null);
        result.setAdmissibilityCheckDate(null);
        Assertions.assertEquals(expectedResult, result);

        Assertions.assertEquals(new Family(family.getFamilyId(), family.getMemberIds()), request.getFamily());
    }

    @Test
    void givenInitiativeConfigRankingWhenHandlerThenSkipAlreadyRankingFamilyOnBoardingException(){
        //Given
        initiativeConfig.setRankingInitiative(true);
        family.setStatus(OnboardingFamilyEvaluationStatus.ONBOARDING_OK);

        //When
        Mono<EvaluationDTO> mono = service.handleExistentFamily(request, family, initiativeConfig, message);

        //Then
        Assertions.assertThrows(SkipAlreadyRankingFamilyOnBoardingException.class,mono::block);
    }

    @Test
    void mapFamilyMemberAlreadyOnboardingResultTest(){
        OnboardingDTO onboardingRequest = OnboardingDTOFaker.mockInstance(1, initiativeConfig.getInitiativeId());
        onboardingRequest.setStatus("ON_EVALUATION");

        EvaluationDTO result = service.mapFamilyMemberAlreadyOnboardingResult(onboardingRequest, "FAMILY_ID", initiativeConfig).block();

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(EvaluationCompletedDTO.class, result);
        EvaluationCompletedDTO resultCompleted = (EvaluationCompletedDTO) result;
        Assertions.assertEquals(OnboardingEvaluationStatus.JOINED, resultCompleted.getStatus());
    }

    @Test
    void handleExistentFamilyCreateTest_inProgressFamily(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.IN_PROGRESS);
        Instant requestDateTime = Instant.now();

        // When
        Mono<EvaluationDTO> mono = service.handleExistentFamilyCreate(request, family, initiativeConfig, message);
        Assertions.assertThrows(WaitingFamilyOnBoardingException.class, mono::block);

        Assertions.assertEquals(new Family(family.getFamilyId(), family.getMemberIds()), request.getFamily());

        Mockito.verify(onboardingRescheduleServiceMock, Mockito.never()).reschedule(
                Mockito.same(request),
                Mockito.argThat(dt -> dt.isAfter(requestDateTime) && dt.isBefore(requestDateTime.plus(2,ChronoUnit.MINUTES))),
                Mockito.eq("Family FAMILYID onboarding IN_PROGRESS into initiative INITIATIVEID"),
                Mockito.same(message));
    }

    @Test
    void handleExistentFamilyCreateTest_inProgressFamilyUserCreated(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.IN_PROGRESS);
        family.setCreateBy(request.getUserId());

        // When
        Mono<EvaluationDTO> mono = service.handleExistentFamilyCreate(request, family, initiativeConfig, message);
        Assertions.assertNull(mono.block());

        Mockito.verifyNoMoreInteractions(onboardingRescheduleServiceMock);
    }

    @Test
    void handleExistentFamilyCreateTest_familyAlreadyProcessed(){
        // Given
        family.setStatus(OnboardingFamilyEvaluationStatus.ONBOARDING_OK);
        family.setCreateBy(request.getUserId());

        // When
        Mono<EvaluationDTO> mono = service.handleExistentFamilyCreate(request, family, initiativeConfig, message);
        Assertions.assertThrows(FamilyAlreadyOnBoardingException.class, mono::block);

        Assertions.assertEquals(new Family(family.getFamilyId(), family.getMemberIds()), request.getFamily());

        Mockito.verifyNoMoreInteractions(onboardingRescheduleServiceMock);
    }
}
