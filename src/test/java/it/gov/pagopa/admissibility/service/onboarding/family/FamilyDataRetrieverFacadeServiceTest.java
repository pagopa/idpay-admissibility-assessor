package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.repository.AnprInfoRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.*;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.AnprInfo;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverServiceImpl;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FamilyDataRetrieverFacadeServiceTest {

    @Mock private FamilyDataRetrieverService familyDataRetrieverServiceMock;
    @Mock private OnboardingFamiliesRepository repositoryMock;
    @Mock private ExistentFamilyHandlerService existentFamilyHandlerServiceMock;
    @Mock private CriteriaCodeService criteriaCodeServiceMock;

    @Mock private  AnprC021RestClient anprC021RestClientMock;

    @Mock private  UserFiscalCodeService userFiscalCodeServiceMock;

    private final Onboarding2EvaluationMapper evaluationMapper = new Onboarding2EvaluationMapper();

    private FamilyDataRetrieverFacadeService service;

    private FamilyDataRetrieverService familyDataRetrieverService;

    @Mock private PagoPaAnprPdndConfig pdndInitiativeConfigMock;

    @Mock private AnprInfoRepository anprInfoRepositoryMock;

    @BeforeEach
    void init(){
        service = new FamilyDataRetrieverFacadeServiceImpl(familyDataRetrieverServiceMock, repositoryMock, existentFamilyHandlerServiceMock, criteriaCodeServiceMock, evaluationMapper);
        familyDataRetrieverService = new FamilyDataRetrieverServiceImpl(anprC021RestClientMock, pdndInitiativeConfigMock, userFiscalCodeServiceMock, anprInfoRepositoryMock);
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(familyDataRetrieverServiceMock, repositoryMock, existentFamilyHandlerServiceMock, criteriaCodeServiceMock);
    }

    private final OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
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
        assertNotNull(result);

        // the mapper use now()
        Assertions.assertFalse(expectedResult.getAdmissibilityCheckDate().isAfter(result.getAdmissibilityCheckDate()));
        expectedResult.setAdmissibilityCheckDate(null);
        result.setAdmissibilityCheckDate(null);
        assertEquals(expectedResult, result);

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
        assertNull(result);
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
        Mockito.when(repositoryMock.findById("FAMILYID_INITIATIVEID")).thenReturn(Mono.just(onboardingFamilies));
        Mockito.when(existentFamilyHandlerServiceMock.handleExistentFamily(request, onboardingFamilies, initiativeConfig, message))
                        .thenReturn(Mono.just(expectedEvaluationResult));

        // When
        EvaluationDTO result = service.retrieveFamily(request, initiativeConfig, message).block();

        // Then
        Assertions.assertSame(result, expectedEvaluationResult);
        Assertions.assertSame(request.getFamily(), family);
    }

    @Test
    void testRetrieveFamily_OK(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();
        TipoListaSoggettiDTO listaSoggettiDTO = new TipoListaSoggettiDTO();
        TipoDatiSoggettiEnteDTO  datiSoggettiEnteDTO = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO generalitaDTO = new TipoGeneralitaDTO();
        TipoCodiceFiscaleDTO codiceFiscaleDTO = new TipoCodiceFiscaleDTO();

        codiceFiscaleDTO.setCodFiscale(fiscalCode);
        generalitaDTO.setCodiceFiscale(codiceFiscaleDTO);
        datiSoggettiEnteDTO.setGeneralita(generalitaDTO);
        listaSoggettiDTO.addDatiSoggettoItem(datiSoggettiEnteDTO);
        eoo20kDTO.setListaSoggetti(listaSoggettiDTO);
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Family familyTest = new Family();
        familyTest.setFamilyId(idOperazioneANPR);
        familyTest.setMemberIds(Set.of(fiscalCodeHashed));

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null))
                .expectNext(Optional.of(familyTest))
                .verifyComplete();
    }





    @Test
    void testGetterSetter() {
        AnprInfo info = new AnprInfo();
        info.setFamilyId("testFamilyId");
        info.setInitiativeId("testInitiativeId");
        info.setUserId("testUserId");
        Set<String> childListIds = new HashSet<>();
        childListIds.add("childListId1");
        childListIds.add("childListId2");
        info.setChildListIds(childListIds);

        assertEquals("testFamilyId", info.getFamilyId());
        assertEquals("testInitiativeId", info.getInitiativeId());
        assertEquals("testUserId", info.getUserId());
        assertEquals(childListIds, info.getChildListIds());
    }

    @Test
    void testBuilder() {
        AnprInfo info = AnprInfo.hiddenBuilder()
                .familyId("testFamilyId")
                .initiativeId("testInitiativeId")
                .userId("testUserId")
                .childListIds(new HashSet<>())
                .hiddenBuild();

        assertEquals("testFamilyId", info.getFamilyId());
        assertEquals("testInitiativeId", info.getInitiativeId());
        assertEquals("testUserId", info.getUserId());
        assertEquals(new HashSet<>(), info.getChildListIds());
    }
}

