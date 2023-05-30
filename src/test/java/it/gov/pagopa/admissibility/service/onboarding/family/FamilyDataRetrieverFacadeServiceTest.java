package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class FamilyDataRetrieverFacadeServiceTest {

    @Mock private FamilyDataRetrieverService familyDataRetrieverServiceMock;
    @Mock private OnboardingFamiliesRepository repositoryMock;
    @Mock private ExistentFamilyHandlerService existentFamilyHandlerServiceMock;
    @Mock private CriteriaCodeService criteriaCodeServiceMock;

    private final Onboarding2EvaluationMapper evaluationMapper = new Onboarding2EvaluationMapper();

    private FamilyDataRetrieverFacadeService service;

    @BeforeEach
    void init(){
        service = new FamilyDataRetrieverFacadeServiceImpl(familyDataRetrieverServiceMock, repositoryMock, existentFamilyHandlerServiceMock, criteriaCodeServiceMock, evaluationMapper);

        CriteriaCodeConfigFaker.mockedCriteriaCodes.forEach(c -> Mockito.lenient().when(criteriaCodeServiceMock.getCriteriaCodeConfig(c.getCode())).thenReturn(c));
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(familyDataRetrieverServiceMock, repositoryMock, existentFamilyHandlerServiceMock, criteriaCodeServiceMock);
    }

    private final OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, 1);
    private final Family family = new Family("FAMILYID", Set.of(request.getUserId()));
    private final InitiativeConfig initiativeConfig = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .build();
    private final OnboardingFamilies onboardingFamilies = new OnboardingFamilies(family, request.getInitiativeId());
    private final Message<String> message = MessageBuilder.withPayload("").build();

    @Test
    void testMonoEmptyFamily(){
        testNoFamily(Mono.empty());
    }

    @Test
    void testOptionalEmptyFamily(){
        testNoFamily(Mono.just(Optional.empty()));
    }

    private void testNoFamily(Mono<Optional<Family>> noFamilyResult) {
        // Given
        Mockito.when(familyDataRetrieverServiceMock.retrieveFamily(request, message)).thenReturn(noFamilyResult);
        EvaluationDTO expectedResult = evaluationMapper.apply(request, initiativeConfig, List.of(new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_KO, OnboardingConstants.REJECTION_REASON_FAMILY_KO, CriteriaCodeConfigFaker.CRITERIA_CODE_FAMILY_AUTH, CriteriaCodeConfigFaker.CRITERIA_CODE_FAMILY_AUTH_LABEL, "Nucleo familiare non disponibile")));

        // When
        EvaluationDTO result = service.retrieveFamily(request, initiativeConfig, message).block();

        // Then
        Assertions.assertNotNull(result);

        // the mapper use now()
        Assertions.assertFalse(expectedResult.getAdmissibilityCheckDate().isAfter(result.getAdmissibilityCheckDate()));
        expectedResult.setAdmissibilityCheckDate(null);
        result.setAdmissibilityCheckDate(null);
        Assertions.assertEquals(expectedResult, result);

        Mockito.verify(criteriaCodeServiceMock).getCriteriaCodeConfig(CriteriaCodeConfigFaker.CRITERIA_CODE_FAMILY);
    }

    @Test
    void testNewFamilyNoCached() {
        Mockito.when(familyDataRetrieverServiceMock.retrieveFamily(request, message)).thenReturn(Mono.just(Optional.of(family)));
        request.setFamily(null);
        testNewFamily();
    }
    @Test
    void testNewFamilyCached() {
        request.setFamily(family);
        testNewFamily();
    }

    void testNewFamily() {
        // Given
        Mockito.when(repositoryMock.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(family, request.getInitiativeId()))
                        .thenReturn(Mono.just(onboardingFamilies));

        // When
        EvaluationDTO result = service.retrieveFamily(request, initiativeConfig, message).block();

        // Then
        Assertions.assertNull(result);
        Assertions.assertSame(request.getFamily(), family);
    }

    @Test
    void testFamilyAlreadyOnboardedNoCached() {
        Mockito.when(familyDataRetrieverServiceMock.retrieveFamily(request, message)).thenReturn(Mono.just(Optional.of(family)));
        request.setFamily(null);

        testFamilyAlreadyOnboarded();
    }
    @Test
    void testFamilyAlreadyOnboardedCached() {
        request.setFamily(family);

        testFamilyAlreadyOnboarded();
    }

    void testFamilyAlreadyOnboarded() {
        // Given
        EvaluationCompletedDTO expectedEvaluationResult = new EvaluationCompletedDTO();

        Mockito.when(repositoryMock.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(family, request.getInitiativeId()))
                .thenReturn(Mono.empty());
        Mockito.when(repositoryMock.findById("FAMILYID_id_0")).thenReturn(Mono.just(onboardingFamilies));
        Mockito.when(existentFamilyHandlerServiceMock.handleExistentFamily(request, onboardingFamilies, initiativeConfig, message))
                        .thenReturn(Mono.just(expectedEvaluationResult));

        // When
        EvaluationDTO result = service.retrieveFamily(request, initiativeConfig, message).block();

        // Then
        Assertions.assertSame(result, expectedEvaluationResult);
        Assertions.assertSame(request.getFamily(), family);
    }
}
