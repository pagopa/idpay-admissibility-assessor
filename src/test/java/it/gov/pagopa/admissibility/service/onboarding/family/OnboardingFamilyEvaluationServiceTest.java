package it.gov.pagopa.admissibility.service.onboarding.family;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.connector.repository.onboarding.OnboardingRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.exception.FamilyAlreadyOnBoardingException;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import it.gov.pagopa.admissibility.model.onboarding.OnboardingFamilyInfo;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class OnboardingFamilyEvaluationServiceTest {

    @Mock private OnboardingFamiliesRepository onboardingFamiliesRepositoryMock;
    @Mock private ExistentFamilyHandlerService existentFamilyHandlerServiceMock;
    @Mock private FamilyDataRetrieverFacadeService familyDataRetrieverFacadeServiceMock;
    @Mock private OnboardingRepository onboardingRepositoryMock;

    private OnboardingFamilyEvaluationService service;

    private final Onboarding2EvaluationMapper mapper = new Onboarding2EvaluationMapper();

    @BeforeEach
    void init(){
        service = new OnboardingFamilyEvaluationServiceImpl(onboardingFamiliesRepositoryMock, existentFamilyHandlerServiceMock, familyDataRetrieverFacadeServiceMock, onboardingRepositoryMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(onboardingFamiliesRepositoryMock, existentFamilyHandlerServiceMock, familyDataRetrieverFacadeServiceMock);
    }

    @Test
    void testNewFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(request.getUserId(), request.getInitiativeId())).thenReturn(Flux.empty());
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage))).thenReturn(Mono.just(expectedResult));

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then

        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testExistentFamily(boolean retrieveFamily){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
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
        EvaluationDTO result = service.checkOnboardingFamily(request, initiativeConfig, expectedMessage, retrieveFamily).block();

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

    @Test
    void testNewFamilyNotRetrieve(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(request.getUserId(), request.getInitiativeId()))
                .thenReturn(Flux.empty());

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, initiativeConfig, expectedMessage, false).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_NewFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("FAMILY_1");
        expectedResult.setMemberIds(Set.of(request.getUserId(), "ANOTHER_USER_1"));
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Family family = new Family("FAMILY_1", Set.of(request.getUserId(), "ANOTHER_USER_1"));
        request.setFamily(family);
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage)))
                .thenReturn(Mono.empty());

        Set<String> onboardingIds = new HashSet<>(Set.of("ANOTHER_USER_1")).stream().map(u -> Onboarding.buildId(request.getInitiativeId(), u)).collect(Collectors.toSet());
        Mockito.when(onboardingRepositoryMock.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name()))
                .thenReturn(Flux.empty());

        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then

        Assertions.assertNull(result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_alreadyFamilyOKWithoutNoMemberOnboarded(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("FAMILY_1");
        expectedResult.setMemberIds(Set.of(request.getUserId(), "ANOTHER_USER"));
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Family family = new Family(expectedResult.getFamilyId(), Set.of(request.getUserId(), "ANOTHER_USER"));
        request.setFamily(family);

        Mockito.when(familyDataRetrieverFacadeServiceMock
                .retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage))
        ).thenReturn(Mono.error(new FamilyAlreadyOnBoardingException()));

        Set<String> onboardingIds = new HashSet<>(Set.of("ANOTHER_USER")).stream().map(u -> Onboarding.buildId(request.getInitiativeId(), u)).collect(Collectors.toSet());
        Mockito.when(onboardingRepositoryMock.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name()))
                .thenReturn(Flux.empty());

        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_alreadyFamilyOKWithAnotherMemberOnboarded(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("FAMILY_1");
        expectedResult.setMemberIds(Set.of(request.getUserId(), "ANOTHER_USER"));
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Family family = new Family(expectedResult.getFamilyId(), Set.of(request.getUserId(), "ANOTHER_USER"));
        request.setFamily(family);

        Mockito.when(familyDataRetrieverFacadeServiceMock
                .retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage))
        ).thenReturn(Mono.error(new FamilyAlreadyOnBoardingException()));

        OnboardingFamilyInfo onboardingView = new OnboardingFamilyInfo() {
            @Override public String getId() { return "ANOTHER_USER_FAMILY_OF_ANOTHER_USER"; }
            @Override public String getUserId() { return "ANOTHER_USER"; }
            @Override public String getFamilyId() { return "FAMILY_OF_ANOTHER_USER"; }
            @Override public String getInitiativeId() { return request.getInitiativeId(); }
            @Override public String getStatus() { return OnboardingEvaluationStatus.ONBOARDING_OK.name(); }
        };
        Set<String> onboardingIds = new HashSet<>(Set.of("ANOTHER_USER")).stream().map(u -> Onboarding.buildId(request.getInitiativeId(), u)).collect(Collectors.toSet());
        Mockito.when(onboardingRepositoryMock.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name()))
                .thenReturn(Flux.just(onboardingView));
        Mockito.when(existentFamilyHandlerServiceMock.mapFamilyMemberAlreadyOnboardingResult(request, request.getFamily().getFamilyId(), initiativeConfig))
                        .thenReturn(Mono.just(expectedResult));
        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_alreadyFamilyInProgress(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("FAMILY_1");
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Mockito.when(familyDataRetrieverFacadeServiceMock
                        .retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage)))
                .thenReturn(Mono.error(new WaitingFamilyOnBoardingException()));

        // When
        Mono<EvaluationDTO> monoResult = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true);

        // Then
        Assertions.assertThrows(WaitingFamilyOnBoardingException.class, monoResult::block);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_NoRetrieveFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, false).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_familyChangeAndAnotherUserOnboarded(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("NEW_FAMILY");
        expectedResult.setMemberIds(Set.of(request.getUserId(), "ANOTHER_USER_2"));

        Family family = new Family("NEW_FAMILY", Set.of(request.getUserId(), "ANOTHER_USER_2"));
        request.setFamily(family);

        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage)))
                .thenReturn(Mono.empty());

        OnboardingFamilyInfo onboardingView = new OnboardingFamilyInfo() {
            @Override public String getId() { return "ANOTHER_USER_FAMILY_OF_ANOTHER_USER"; }
            @Override public String getUserId() { return "ANOTHER_USER"; }
            @Override public String getFamilyId() { return "FAMILY_OF_ANOTHER_USER"; }
            @Override public String getInitiativeId() { return request.getInitiativeId(); }
            @Override public String getStatus() { return OnboardingEvaluationStatus.ONBOARDING_OK.name(); }
        };
        Set<String> onboardingIds = new HashSet<>(Set.of("ANOTHER_USER_2")).stream().map(u -> Onboarding.buildId(request.getInitiativeId(), u)).collect(Collectors.toSet());
        Mockito.when(onboardingRepositoryMock.findByIdInAndStatus(onboardingIds,OnboardingEvaluationStatus.ONBOARDING_OK.name()))
                .thenReturn(Flux.just(onboardingView));

        Mockito.when(existentFamilyHandlerServiceMock.mapFamilyMemberAlreadyOnboardingResult(
                Mockito.same(request), Mockito.same("NEW_FAMILY"), Mockito.same(initiativeConfig))).thenReturn(Mono.just(expectedResult));

        UpdateResult updateResultMock = Mockito.mock(UpdateResult.class);
        Mockito.when(onboardingFamiliesRepositoryMock.updateOnboardingFamilyOutcome(request.getFamily(), initiativeConfig.getInitiativeId(), OnboardingFamilyEvaluationStatus.ONBOARDING_OK, Collections.emptyList()))
                .thenReturn(Mono.just(updateResultMock));
        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testRetrieveAndCheckOnboardingFamily_familyChange(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
        InitiativeConfig initiativeConfig = new InitiativeConfig();

        EvaluationDTO expectedResult = mapper.apply(request, initiativeConfig, Collections.emptyList());
        expectedResult.setFamilyId("NEW_FAMILY");
        expectedResult.setMemberIds(Set.of(request.getUserId(), "ANOTHER_USER_1", "ANOTHER_USER_2"));
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Family family = new Family("NEW_FAMILY", Set.of(request.getUserId(), "ANOTHER_USER_1", "ANOTHER_USER_2"));
        request.setFamily(family);
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(initiativeConfig), Mockito.same(expectedMessage)))
                .thenReturn(Mono.empty());

        Set<String> onboardingIds = new HashSet<>(Set.of("ANOTHER_USER_1", "ANOTHER_USER_2")).stream().map(u -> Onboarding.buildId(request.getInitiativeId(), u)).collect(Collectors.toSet());
        Mockito.when(onboardingRepositoryMock.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name()))
                .thenReturn(Flux.empty());

        // When
        EvaluationDTO result = service.retrieveAndCheckOnboardingFamily(request, initiativeConfig, expectedMessage, true).block();

        // Then
        Assertions.assertNull(result);
    }
}
