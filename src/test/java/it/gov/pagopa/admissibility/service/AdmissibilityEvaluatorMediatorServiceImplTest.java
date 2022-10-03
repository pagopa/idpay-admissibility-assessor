package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRequestEvaluatorService;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
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

        AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService = new AdmissibilityEvaluatorMediatorServiceImpl(onboardingCheckService, authoritiesDataRetrieverService, onboardingRequestEvaluatorServiceMock, onboarding2EvaluationMapper, errorNotifierServiceMock, TestUtils.objectMapper);

        OnboardingDTO onboarding1 = OnboardingDTO.builder().userId("USER1").build();
        OnboardingDTO onboarding2 = OnboardingDTO.builder().userId("USER2").build();

        List<Message<String>> msgs = Stream.of(onboarding1, onboarding2).map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build).collect(Collectors.toList());

        Flux<Message<String>> onboardingFlux = Flux.fromIterable(msgs);

        Mockito.when(onboardingCheckService.check(Mockito.eq(onboarding1), Mockito.any())).thenReturn(null);
        Mockito.when(onboardingCheckService.check(Mockito.eq(onboarding2), Mockito.any())).thenReturn(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                .code("Rejected")
                .build());

        Mockito.when(authoritiesDataRetrieverService.retrieve(Mockito.eq(onboarding1), Mockito.any(), Mockito.eq(msgs.get(0)))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        Mockito.when(onboardingRequestEvaluatorServiceMock.evaluate(Mockito.eq(onboarding1), Mockito.any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        // When
        List<EvaluationDTO> result = admissibilityEvaluatorMediatorService.execute(onboardingFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());

        Mockito.verifyNoInteractions(errorNotifierServiceMock);
    }
}
