package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
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

    @Test
    void testNewFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
        EvaluationDTO expectedResult = mapper.apply(request, new InitiativeConfig(), Collections.emptyList());
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsIn(request.getUserId())).thenReturn(Flux.empty());
        Mockito.when(familyDataRetrieverFacadeServiceMock.retrieveFamily(Mockito.same(request), Mockito.same(expectedMessage))).thenReturn(Mono.just(expectedResult));

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, expectedMessage).block();

        // Then

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testExistentFamily(){
        // Given
        OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
        EvaluationDTO expectedResult = mapper.apply(request, new InitiativeConfig(), Collections.emptyList());
        @SuppressWarnings("unchecked") Message<String> expectedMessage = Mockito.mock(Message.class);

        OnboardingFamilies f1 = OnboardingFamilies.builder()
                .id("TESTFAMILYID")
                .memberIds(Set.of("ID1", "ID2"))
                .createDate(LocalDateTime.now())
                .build();

        OnboardingFamilies f2 = OnboardingFamilies.builder()
                .id("TESTFAMILYID2")
                .memberIds(Set.of("ID2", "ID3"))
                .createDate(LocalDateTime.now().plusMinutes(2))
                .build();

        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsIn(request.getUserId())).thenReturn(Flux.just(f1, f2));
        Mockito.when(existentFamilyHandlerServiceMock.handleExistentFamily(Mockito.same(request), Mockito.same(f2), Mockito.same(expectedMessage))).thenReturn(Mono.just(expectedResult));

        // When
        EvaluationDTO result = service.checkOnboardingFamily(request, expectedMessage).block();

        // Then
        Assertions.assertEquals(expectedResult, result);
    }
}
