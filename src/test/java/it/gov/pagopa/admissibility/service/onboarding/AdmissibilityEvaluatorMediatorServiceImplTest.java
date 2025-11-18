package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsGenericException;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.OnboardingRequestEvaluatorService;
import it.gov.pagopa.admissibility.service.onboarding.family.OnboardingFamilyEvaluationService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.RankingNotifierService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AdmissibilityEvaluatorMediatorServiceImplTest {

    @Mock private OnboardingContextHolderService onboardingContextHolderServiceMock;
    @Mock private OnboardingCheckService onboardingCheckServiceMock;
    @Mock private OnboardingFamilyEvaluationService onboardingFamilyEvaluationServiceMock;
    @Mock private AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceMock;
    @Mock private OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock;
    @Mock private AdmissibilityErrorNotifierService admissibilityErrorNotifierServiceMock;
    @Mock private OnboardingNotifierService onboardingNotifierServiceMock;
    @Mock private RankingNotifierService rankingNotifierServiceMock;

    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

    private AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService;

    private static final int maxRetry = 2;

    @BeforeEach
    void init(){
        admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(maxRetry, onboardingContextHolderServiceMock, onboardingCheckServiceMock, onboardingFamilyEvaluationServiceMock, authoritiesDataRetrieverServiceMock, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, admissibilityErrorNotifierServiceMock, TestUtils.objectMapper, onboardingNotifierServiceMock, rankingNotifierServiceMock);
    }

    @AfterEach
    void verifyNoMoreIvocations(){
        Mockito.mockingDetails(admissibilityErrorNotifierServiceMock).getInvocations()
                .forEach(i-> System.out.println("Called errorNotifier: " + Arrays.toString(i.getArguments())));

        Mockito.verifyNoMoreInteractions(
                onboardingContextHolderServiceMock,
                onboardingCheckServiceMock,
                onboardingFamilyEvaluationServiceMock,
                authoritiesDataRetrieverServiceMock,
                onboardingRequestEvaluatorServiceMock,
                admissibilityErrorNotifierServiceMock,
                onboardingNotifierServiceMock,
                rankingNotifierServiceMock);
    }

    @Test
    void mediatorTest() {
        // Given
        String initiativeId = "INITIATIVEID";
        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").initiativeId(initiativeId).build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").initiativeId(initiativeId).build();

        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId(initiativeId).build();

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        List<Checkpointer> checkpointers= new ArrayList<>(2);
        List<Message<String>> msgs = Stream.of(onboarding1, onboarding2)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                    Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                    Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                    checkpointers.add(checkpointer);
                    return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer);
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding2), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                .code("Rejected")
                .build());

        EvaluationCompletedDTO evaluationDTO1 = EvaluationCompletedDTO.builder().userId("USER1").status(OnboardingEvaluationStatus.ONBOARDING_KO).build();

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenAnswer(i -> Mono.just(evaluationDTO1));

        Mockito.when(onboardingRequestEvaluatorServiceMock.updateInitiativeBudget(Mockito.any(), Mockito.eq(initiativeConfig))).thenAnswer(a -> Mono.just(a.getArguments()[0]));

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(admissibilityErrorNotifierServiceMock, onboardingFamilyEvaluationServiceMock);
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock).evaluate(Mockito.any(), Mockito.any());
        checkCommits(checkpointers);
    }

    @Test
    void mediatorTestWhenNoInitiative() {
        // Given
        String initiativeId = "INITIATIVEID";
        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").initiativeId(initiativeId).build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").initiativeId(initiativeId).build();

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.empty());

        List<Checkpointer> checkpointers= new ArrayList<>(2);
        List<Message<String>> msgs = Stream.of(onboarding1, onboarding2)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                            Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                            Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                            checkpointers.add(checkpointer);
                            return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer);
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.any(), Mockito.isNull(), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code(OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL)
                .build());

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(admissibilityErrorNotifierServiceMock,onboardingFamilyEvaluationServiceMock);
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        checkCommits(checkpointers);
    }

    @Test
    void mediatorErrorSenderTest() {
        // Given
        String initiativeId = "INITIATIVEID";
        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").initiativeId(initiativeId).build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").initiativeId(initiativeId).build();

        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId(initiativeId).build();

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        List<Checkpointer> checkpointers= new ArrayList<>(2);
        List<Message<String>> msgs = Stream.of(onboarding1, onboarding2)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                            Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                            Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                            checkpointers.add(checkpointer);
                            return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer);
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding2), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(null);

        EvaluationCompletedDTO evaluationDTO1 = EvaluationCompletedDTO.builder().userId("USER1").status(OnboardingEvaluationStatus.ONBOARDING_KO).build();
        EvaluationCompletedDTO evaluationDTO2 = EvaluationCompletedDTO.builder().userId("USER2").status(OnboardingEvaluationStatus.ONBOARDING_KO).build();

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenAnswer(i -> Mono.just(evaluationDTO1));

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding2), Mockito.any(), Mockito.eq(msgs.get(1)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding2), Mockito.any())).thenAnswer(i -> Mono.just(evaluationDTO2));

        Mockito.when(onboardingRequestEvaluatorServiceMock.updateInitiativeBudget(Mockito.any(), Mockito.eq(initiativeConfig))).thenAnswer(a -> Mono.just(a.getArguments()[0]));

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.same(evaluationDTO1))).thenReturn(false);
        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.same(evaluationDTO2))).thenThrow(new RuntimeException());

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(onboardingFamilyEvaluationServiceMock);
        Mockito.verify(admissibilityErrorNotifierServiceMock, Mockito.times(2)).notifyAdmissibilityOutcome(Mockito.any(GenericMessage.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        Mockito.verify(admissibilityErrorNotifierServiceMock).notifyAdmissibilityOutcome(Mockito.any(GenericMessage.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(IllegalStateException.class));
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock, Mockito.times(2)).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock, Mockito.times(2)).evaluate(Mockito.any(), Mockito.any());
        Assertions.assertEquals(2, checkpointers.size());
        checkCommits(checkpointers);
    }


    @Test
    void mediatorTestWhenFamilyInitiative() {
        // Given
        String initiativeId = "INITIATIVEID";
        OnboardingDTO onboarding_first = OnboardingDTO.builder().userId("USER1_FIRST_FAMILY_MEMBER").initiativeId(initiativeId).build();
        OnboardingDTO onboarding_waitingFirst = OnboardingDTO.builder().userId("USER2_WAITING_FAMILY").initiativeId(initiativeId).build();
        OnboardingDTO onboarding_familyOk = OnboardingDTO.builder().userId("USER2_FAMILY_OK").initiativeId(initiativeId).build();
        OnboardingDTO onboarding_familyKo = OnboardingDTO.builder().userId("USER3_FAMILY_KO").initiativeId(initiativeId).build();

        Family family1 = new Family("FAMILY1", Set.of(onboarding_first.getUserId()));
        Family family2 = new Family("FAMILY2", Set.of(onboarding_waitingFirst.getUserId()));
        Family family3 = new Family("FAMILY3", Set.of(onboarding_familyOk.getUserId()));
        Family family4 = new Family("FAMILY4", Set.of(onboarding_familyKo.getUserId()));


        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .rankingInitiative(false)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
                .build();

        EvaluationDTO expectedEvaluationOnboardingFirst = onboarding2EvaluationMapper.apply(onboarding_first, initiativeConfig, Collections.emptyList());
        EvaluationDTO expectedEvaluationOnboardingFamilyOk = onboarding2EvaluationMapper.apply(onboarding_familyOk, initiativeConfig, Collections.emptyList());
        EvaluationDTO expectedEvaluationOnboardingFamilyKo = onboarding2EvaluationMapper.apply(onboarding_familyKo, initiativeConfig, new ArrayList<>(List.of(new OnboardingRejectionReason())));

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        List<Checkpointer> checkpointers= new ArrayList<>(4);
        List<Message<String>> msgs = Stream.of(onboarding_first, onboarding_waitingFirst, onboarding_familyOk, onboarding_familyKo)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                            Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                            Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                            checkpointers.add(checkpointer);
                            return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer);
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.any(), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(null);

        Mockito.when(onboardingFamilyEvaluationServiceMock.retrieveAndCheckOnboardingFamily(onboarding_first, initiativeConfig, msgs.get(0), true)).thenAnswer(i -> {
            i.getArgument(0, OnboardingDTO.class).setFamily(family1);
            onboarding_first.setFamily(family1);
            return Mono.empty();
        });
        Mockito.when(onboardingFamilyEvaluationServiceMock.retrieveAndCheckOnboardingFamily(onboarding_waitingFirst, initiativeConfig, msgs.get(1), true)).thenAnswer(i -> {
            i.getArgument(0, OnboardingDTO.class).setFamily(family2);
            onboarding_waitingFirst.setFamily(family2);
            return Mono.error(new WaitingFamilyOnBoardingException());
        });
        Mockito.when(onboardingFamilyEvaluationServiceMock.retrieveAndCheckOnboardingFamily(onboarding_familyOk, initiativeConfig, msgs.get(2), true)).thenAnswer(i -> {
            i.getArgument(0, OnboardingDTO.class).setFamily(family3);
            onboarding_familyOk.setFamily(family3);
            return Mono.just(expectedEvaluationOnboardingFamilyOk);
        });
        Mockito.when(onboardingFamilyEvaluationServiceMock.retrieveAndCheckOnboardingFamily(onboarding_familyKo, initiativeConfig, msgs.get(3), true)).thenAnswer(i -> {
            i.getArgument(0, OnboardingDTO.class).setFamily(family4);
            onboarding_familyKo.setFamily(family4);
            return Mono.just(expectedEvaluationOnboardingFamilyKo);
        });

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(i -> Mono.error(new IllegalStateException("UNEXPECTED")));

        Mockito.doAnswer(i -> Mono.just(i.getArgument(0)))
                .when(authoritiesDataRetrieverServiceMock)
                .retrieve(Mockito.eq(onboarding_first), Mockito.same(initiativeConfig), Mockito.same(msgs.get(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding_first), Mockito.any())).thenAnswer(i -> Mono.just(expectedEvaluationOnboardingFirst));
        Mockito.when(onboardingFamilyEvaluationServiceMock.updateOnboardingFamilyOutcome(Mockito.same(family1), Mockito.eq(initiativeConfig), Mockito.same(expectedEvaluationOnboardingFirst))).thenAnswer(i -> Mono.just(expectedEvaluationOnboardingFirst));

        Mockito.when(onboardingRequestEvaluatorServiceMock.updateInitiativeBudget(Mockito.any(), Mockito.eq(initiativeConfig))).thenAnswer(a -> Mono.just(a.getArguments()[0]));

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(admissibilityErrorNotifierServiceMock);

        Mockito.verify(onboardingCheckServiceMock).check(Mockito.eq(onboarding_first), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(onboardingCheckServiceMock).check(Mockito.eq(onboarding_waitingFirst), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(onboardingCheckServiceMock).check(Mockito.eq(onboarding_familyOk), Mockito.same(initiativeConfig), Mockito.any());
        Mockito.verify(onboardingCheckServiceMock).check(Mockito.eq(onboarding_familyKo), Mockito.same(initiativeConfig), Mockito.any());

        Mockito.verify(onboardingNotifierServiceMock).notify(expectedEvaluationOnboardingFirst);
        Mockito.verify(onboardingNotifierServiceMock).notify(expectedEvaluationOnboardingFamilyOk);
        Mockito.verify(onboardingNotifierServiceMock).notify(expectedEvaluationOnboardingFamilyKo);

        Mockito.verify(authoritiesDataRetrieverServiceMock).retrieve(Mockito.eq(onboarding_first), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock).evaluate(Mockito.eq(onboarding_first), Mockito.any());

        checkCommits(checkpointers);
    }

    @Test
    void mediatorTestWhenFamilyOuterInitiative() {
        // Given
        String initiativeId = "INITIATIVEID";
        String userId = "USERID";
        OnboardingDTO onboarding = OnboardingDTO.builder().userId(userId).initiativeId(initiativeId).build();

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .rankingInitiative(true)
                .rankingFields(List.of(Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()))
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
                .build();

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        List<Checkpointer> checkpointers= new ArrayList<>(1);
        List<Message<String>> msgs = Stream.of(onboarding)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                            Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                            Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                            checkpointers.add(checkpointer);
                            return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer);
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.any(), Mockito.same(initiativeConfig), Mockito.any()))
                .thenReturn(OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code(OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL).build());

        Mockito.when(onboardingFamilyEvaluationServiceMock.retrieveAndCheckOnboardingFamily(onboarding, initiativeConfig, msgs.get(0), false))
                        .thenReturn(Mono.empty());

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);
        Mockito.when(rankingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(admissibilityErrorNotifierServiceMock);

        Mockito.verify(onboardingCheckServiceMock).check(Mockito.eq(onboarding), Mockito.same(initiativeConfig), Mockito.any());

        Mockito.verify(onboardingNotifierServiceMock).notify(Mockito.any());

        ArgumentCaptor<EvaluationCompletedDTO> argument = ArgumentCaptor.forClass(EvaluationCompletedDTO.class);
        Mockito.verify(onboardingNotifierServiceMock).notify(argument.capture());
        Assertions.assertEquals(argument.getValue().getOnboardingRejectionReasons(), List.of(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code(OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL).build()));

        checkCommits(checkpointers);
    }

    @Test
    void mediatorTestMaxRetryOnboarding(){
        String initiativeId = "INITIATIVEID";
        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").initiativeId(initiativeId).build();

        InitiativeConfig initiativeConfig = InitiativeConfig.builder().initiativeId(initiativeId).build();

        Mockito.when(onboardingContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));

        List<Checkpointer> checkpointers= new ArrayList<>(1);
        List<Message<String>> msgs = Stream.of(onboarding1)
                .map(TestUtils::jsonSerializer)
                .map(s -> {
                            Checkpointer checkpointer = Mockito.mock(Checkpointer.class);
                            Mockito.when(checkpointer.success()).thenReturn(Mono.empty());
                            checkpointers.add(checkpointer);
                            return MessageBuilder.withPayload(s)
                                    .setHeader(AzureHeaders.CHECKPOINTER, checkpointer)
                                    .setHeader(KafkaConstants.ERROR_MSG_HEADER_RETRY, String.valueOf(maxRetry));
                        }
                )
                .map(MessageBuilder::build).toList();

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.same(initiativeConfig), Mockito.any())).thenReturn(null);


        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        EvaluationCompletedDTO evaluationDTO1 = EvaluationCompletedDTO.builder().userId("USER1").status(OnboardingEvaluationStatus.ONBOARDING_KO).build();
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.any(), Mockito.any()))
                .thenAnswer(i -> Mono.error(new InpsGenericException("DUMMY_EXCEPTION", new RuntimeException())))
                .thenReturn(Mono.just(evaluationDTO1));

        Mockito.when(onboardingRequestEvaluatorServiceMock.updateInitiativeBudget(Mockito.any(), Mockito.eq(initiativeConfig))).thenAnswer(a -> Mono.just(a.getArguments()[0]));

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(admissibilityErrorNotifierServiceMock, onboardingFamilyEvaluationServiceMock);
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(1)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock, Mockito.times(2)).evaluate(Mockito.any(), Mockito.any());
        checkCommits(checkpointers);
    }

    private static void checkCommits(List<Checkpointer> checkpointers) {
        TestUtils.wait(100, TimeUnit.MILLISECONDS);
        checkpointers.forEach(c -> Mockito.verify(c).success());
    }
}
