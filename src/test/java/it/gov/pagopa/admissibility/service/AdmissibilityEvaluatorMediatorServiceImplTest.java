package it.gov.pagopa.admissibility.service;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.*;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AdmissibilityEvaluatorMediatorServiceImplTest {

    @Mock private OnboardingContextHolderService onboardingContextHolderServiceMock;
    @Mock private OnboardingCheckService onboardingCheckServiceMock;
    @Mock private AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceMock;
    @Mock private OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;
    @Mock private OnboardingNotifierService onboardingNotifierServiceMock;
    @Mock private RankingNotifierService rankingNotifierServiceMock;

    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

    private AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService;

    @BeforeEach
    void init(){
        admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingContextHolderServiceMock, onboardingCheckServiceMock, authoritiesDataRetrieverServiceMock, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, errorNotifierServiceMock, TestUtils.objectMapper, onboardingNotifierServiceMock, rankingNotifierServiceMock);
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

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock).evaluate(Mockito.any(), Mockito.any());
        checkpointers.forEach(c -> Mockito.verify(c).success());
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

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.isNull(), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding2), Mockito.isNull(), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                .code("Rejected")
                .build());

        EvaluationCompletedDTO evaluationDTO1 = EvaluationCompletedDTO.builder().userId("USER1").status(OnboardingEvaluationStatus.ONBOARDING_KO).build();

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenAnswer(i -> Mono.just(evaluationDTO1));

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock).evaluate(Mockito.any(), Mockito.any());
        checkpointers.forEach(c -> Mockito.verify(c).success());
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

        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.same(evaluationDTO1))).thenReturn(false);
        Mockito.when(onboardingNotifierServiceMock.notify(Mockito.same(evaluationDTO2))).thenThrow(new RuntimeException());

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyAdmissibilityOutcome(Mockito.any(GenericMessage.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        Mockito.verify(errorNotifierServiceMock).notifyAdmissibilityOutcome(Mockito.any(GenericMessage.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(IllegalStateException.class));
        Mockito.verify(onboardingNotifierServiceMock, Mockito.times(2)).notify(Mockito.any());
        Mockito.verify(authoritiesDataRetrieverServiceMock, Mockito.times(2)).retrieve(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(onboardingRequestEvaluatorServiceMock, Mockito.times(2)).evaluate(Mockito.any(), Mockito.any());
        Assertions.assertEquals(2, checkpointers.size());
        checkpointers.forEach(c -> Mockito.verify(c).success());
    }
}
