package it.gov.pagopa.admissibility.service;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.AzureCheckpointer;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.service.onboarding.*;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AdmissibilityEvaluatorMediatorServiceImplTest {

    @Test
    void mediatorTest() {

        // Given
        OnboardingCheckService onboardingCheckService = Mockito.mock(OnboardingCheckServiceImpl.class);
        AuthoritiesDataRetrieverService authoritiesDataRetrieverService = Mockito.mock(AuthoritiesDataRetrieverService.class);
        OnboardingRequestEvaluatorService onboardingRequestEvaluatorServiceMock = Mockito.mock(OnboardingRequestEvaluatorService.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        OnboardingNoifierService onboardingNoifierService = Mockito.mock(OnboardingNoifierService.class);

        AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingCheckService, authoritiesDataRetrieverService, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, errorNotifierServiceMock, TestUtils.objectMapper, onboardingNoifierService);

        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").build();

        List<Message<String>> msgs = Stream.of(onboarding1, onboarding2)
                .map(TestUtils::jsonSerializer)
//                .map(MessageBuilder::withPayload)
                .map(s -> MessageBuilder.withPayload(s)
                        .setHeader(AzureHeaders.CHECKPOINTER, new AzureCheckpointer(Mono::empty))
                )
                .map(MessageBuilder::build).collect(Collectors.toList());

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckService.check(Mockito.eq(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckService.check(Mockito.eq(onboarding2), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                .code("Rejected")
                .build());

        EvaluationDTO evaluationDTO1 = EvaluationDTO.builder().userId("USER1").build();

        Mockito.when(authoritiesDataRetrieverService.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenAnswer(i -> Mono.just(evaluationDTO1));

        Mockito.when(onboardingNoifierService.notify(Mockito.any())).thenReturn(true);

        // When
        admissibilityEvaluatorMediatorService.execute(onboardingFlux);

        // Then
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
        //TODO add more assertions
    }
}
