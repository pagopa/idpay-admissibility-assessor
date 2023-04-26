package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRescheduleService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ExistentFamilyHandlerServiceTest {

    @Mock private OnboardingRescheduleService onboardingRescheduleServiceMock;

    private final Onboarding2EvaluationMapper mapper = new Onboarding2EvaluationMapper();

    private ExistentFamilyHandlerService service;

    @BeforeEach
    void init(){
        service = new ExistentFamilyHandlerServiceImpl(1, new Onboarding2EvaluationMapper(), onboardingRescheduleServiceMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(onboardingRescheduleServiceMock);
    }


    private final OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
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
        OffsetDateTime requestDateTime = OffsetDateTime.now();

        // When
        Mono<EvaluationDTO> mono = service.handleExistentFamily(request, family, initiativeConfig, message);
        Assertions.assertThrows(WaitingFamilyOnBoardingException.class, mono::block);

        Mockito.verify(onboardingRescheduleServiceMock).reschedule(
                Mockito.same(request),
                Mockito.argThat(dt -> dt.isAfter(requestDateTime) && dt.isBefore(requestDateTime.plusMinutes(2))),
                Mockito.eq("Family FAMILYID onboarding IN_PROGRESS into initiative id_0"),
                Mockito.same(message));
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
        //Given
        EvaluationCompletedDTO expectedResult = (EvaluationCompletedDTO) mapper.apply(request, initiativeConfig, family.getOnboardingRejectionReasons());

        // When
        EvaluationDTO result = service.handleExistentFamily(request, family, initiativeConfig, message).block();

        // Then
        expectedResult.setStatus(expectedStatus);
        Assertions.assertNotNull(result);
        // the mapper use now()
        Assertions.assertFalse(expectedResult.getAdmissibilityCheckDate().isAfter(result.getAdmissibilityCheckDate()));
        expectedResult.setAdmissibilityCheckDate(null);
        result.setAdmissibilityCheckDate(null);
        Assertions.assertEquals(expectedResult, result);
    }
}
