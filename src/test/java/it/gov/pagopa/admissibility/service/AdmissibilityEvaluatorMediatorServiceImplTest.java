package it.gov.pagopa.admissibility.service;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Evaluation2RankingRequestMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.service.onboarding.*;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void mediatorTest() {

        // Given
        OnboardingCheckService onboardingCheckServiceMock = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceMock = Mockito.mock(AuthoritiesDataRetrieverService.class);
        OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock = Mockito.mock(OnboardingRequestEvaluatorService.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();
        Evaluation2RankingRequestMapper evaluation2RankingRequestMapperMock = Mockito.mock(Evaluation2RankingRequestMapper.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        OnboardingNotifierService onboardingNotifierServiceMock = Mockito.mock(OnboardingNotifierService.class);
        RankingNotifierService rankingNotifierServiceMock = Mockito.mock(RankingNotifierService.class);

        AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingCheckServiceMock, authoritiesDataRetrieverServiceMock, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, evaluation2RankingRequestMapperMock, errorNotifierServiceMock, TestUtils.objectMapper, onboardingNotifierServiceMock, rankingNotifierServiceMock);

        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").build();

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

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding2), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                .code("Rejected")
                .build());

        EvaluationDTO evaluationDTO1 = EvaluationDTO.builder().userId("USER1").status(OnboardingConstants.ONBOARDING_STATUS_KO).build();

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
        OnboardingCheckService onboardingCheckServiceMock = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceMock = Mockito.mock(AuthoritiesDataRetrieverService.class);
        OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock = Mockito.mock(OnboardingRequestEvaluatorService.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();
        Evaluation2RankingRequestMapper evaluation2RankingRequestMapperMock = Mockito.mock(Evaluation2RankingRequestMapper.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        OnboardingNotifierService onboardingNotifierServiceMock = Mockito.mock(OnboardingNotifierService.class);
        RankingNotifierService rankingNotifierServiceMock = Mockito.mock(RankingNotifierService.class);

        AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingCheckServiceMock, authoritiesDataRetrieverServiceMock, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, evaluation2RankingRequestMapperMock, errorNotifierServiceMock, TestUtils.objectMapper, onboardingNotifierServiceMock, rankingNotifierServiceMock);

        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").build();

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

        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckServiceMock.check(Mockito.eq(onboarding2), Mockito.any())).thenReturn(null);

        EvaluationDTO evaluationDTO1 = EvaluationDTO.builder().userId("USER1").status(OnboardingConstants.ONBOARDING_STATUS_KO).build();
        EvaluationDTO evaluationDTO2 = EvaluationDTO.builder().userId("USER2").status(OnboardingConstants.ONBOARDING_STATUS_KO).build();

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenReturn(Mono.just(evaluationDTO1));

        Mockito.when(authoritiesDataRetrieverServiceMock.retrieve(Mockito.eq(onboarding2), Mockito.any(), Mockito.eq(msgs.get(1)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding2), Mockito.any())).thenReturn(Mono.just(evaluationDTO2));

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
