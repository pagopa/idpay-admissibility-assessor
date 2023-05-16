package it.gov.pagopa.admissibility.service.onboarding.family;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class OnboardingFamilyEvaluationServiceTest {

    @Mock private OnboardingFamiliesRepository onboardingFamiliesRepositoryMock;
    @Mock private ExistentFamilyHandlerService existentFamilyHandlerServiceMock;
    @Mock private FamilyDataRetrieverFacadeService familyDataRetrieverFacadeServiceMock;

    private OnboardingFamilyEvaluationService service;

    private final Onboarding2EvaluationMapper mapper = new Onboarding2EvaluationMapper();

    @BeforeEach
    void init(){
        service = new OnboardingFamilyEvaluationServiceImpl(onboardingFamiliesRepositoryMock, existentFamilyHandlerServiceMock, familyDataRetrieverFacadeServiceMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(onboardingFamiliesRepositoryMock, existentFamilyHandlerServiceMock, familyDataRetrieverFacadeServiceMock);
    }

    @Test
    void testNewFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(request.getUserId(), request.getInitiativeId())).thenReturn(Flux.empty());
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage))).thenReturn(Mono.just(expectedResult));

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, initiativeConfig, expectedMessage).block();

        // Then

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testExistentFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        OnboardingFamilies f1 = OnboardingFamilies.builder(new Family("FAMILYID", Set.of("ID1", "ID2")), request.getInitiativeId())
                .createDate(LocalDateTime.now())
                .build();

        OnboardingFamilies f2 = OnboardingFamilies.builder(new Family("FAMILYID2", Set.of("ID2", "ID3")), request.getInitiativeId())
                .createDate(LocalDateTime.now().plusMinutes(2))
                .build();

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(request.getUserId(), request.getInitiativeId())).thenReturn(Flux.just(f1, f2));
        Mockito.when(existentFamilyHandlerServiceMock.handleExistentFamily(Mockito.same(request), Mockito.same(f2), Mockito.same(initiativeConfig), Mockito.same(expectedMessage))).thenReturn(Mono.just(expectedResult));

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, initiativeConfig, expectedMessage).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    private final Map<OnboardingEvaluationStatus, OnboardingFamilyEvaluationStatus> expectedEvaluationStatusMapping = Map.of(
            OnboardingEvaluationStatus.ONBOARDING_OK, OnboardingFamilyEvaluationStatus.ONBOARDING_OK,
            OnboardingEvaluationStatus.DEMANDED, OnboardingFamilyEvaluationStatus.ONBOARDING_OK,
            OnboardingEvaluationStatus.JOINED, OnboardingFamilyEvaluationStatus.ONBOARDING_OK,
            OnboardingEvaluationStatus.REJECTED, OnboardingFamilyEvaluationStatus.ONBOARDING_KO,
            OnboardingEvaluationStatus.ONBOARDING_KO, OnboardingFamilyEvaluationStatus.ONBOARDING_KO
    );

    @ParameterizedTest
    @EnumSource(OnboardingEvaluationStatus.class)
    void testUpdateOnboardingFamilyOutcome_Completed(OnboardingEvaluationStatus evaluationStatus){
        // Given
        EvaluationCompletedDTO evaluation = new EvaluationCompletedDTO();
        evaluation.setStatus(evaluationStatus);
        evaluation.setOnboardingRejectionReasons(Collections.emptyList());
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        Family family = new Family("FAMILYID", Set.of("USERID"));

        Mockito.when(onboardingFamiliesRepositoryMock.updateOnboardingFamilyOutcome(Mockito.same(family), Mockito.same(initiativeConfig.getInitiativeId()), Mockito.eq(expectedEvaluationStatusMapping.get(evaluationStatus)), Mockito.same(evaluation.getOnboardingRejectionReasons())))
                .thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        EvaluationDTO result = service.updateOnboardingFamilyOutcome(family, initiativeConfig, evaluation).block();

        // Then
        Assertions.assertSame(result, evaluation);
    }

    @Test
    void testUpdateOnboardingFamilyOutcome_Ranking(){
        // Given
        RankingRequestDTO evaluation = new RankingRequestDTO();
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        Family family = new Family("FAMILYID", Set.of("USERID"));

        Mockito.when(onboardingFamiliesRepositoryMock.updateOnboardingFamilyOutcome(Mockito.same(family), Mockito.same(initiativeConfig.getInitiativeId()), Mockito.eq(OnboardingFamilyEvaluationStatus.ONBOARDING_OK), Mockito.eq(Collections.emptyList())))
                .thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        EvaluationDTO result = service.updateOnboardingFamilyOutcome(family, initiativeConfig, evaluation).block();

        // Then
        Assertions.assertSame(result, evaluation);
    }
}
