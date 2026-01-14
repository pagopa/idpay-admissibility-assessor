package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndOkResponse;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaKODTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoDatiSoggettiEnteDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoIndirizzoDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoListaSoggettiDTO;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClientImplIntegrationTest.FISCAL_CODE_OK;
import static it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClientImplIntegrationTest.PDND_INITIATIVE_CONFIG;

@ExtendWith(MockitoExtension.class)
class AnprDataRetrieverServiceImplTest {

    @Mock
    private AnprC001RestClient anprC001RestClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private RispostaE002OKDTO anprOkAnswer;
    private PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> anprAnswer;
    private Residence expectedResidence;
    private BirthDate expectedBirthDate;

    private AnprDataRetrieverService service;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        service = new AnprDataRetrieverServiceImpl(anprC001RestClientMock, criteriaCodeServiceMock, residenceMapper);

        anprOkAnswer = AnprC001RestClientImplIntegrationTest.buildExpectedResponse();
        TipoDatiSoggettiEnteDTO returnedSubject = anprOkAnswer.getListaSoggetti().getDatiSoggetto().get(0);

        TipoIndirizzoDTO returnedAddress = returnedSubject.getResidenza().get(0).getIndirizzo();
        expectedResidence = Residence.builder()
                .cityCouncil(returnedAddress.getComune().getNomeComune())
                .city(returnedAddress.getComune().getNomeComune())
                .province(returnedAddress.getComune().getSiglaProvinciaIstat())
                .postalCode(returnedAddress.getCap())
                .build();

        anprAnswer = new PdndOkResponse<>(anprOkAnswer);

        String birthYear = returnedSubject.getGeneralita().getDataNascita().substring(0, 4);
        expectedBirthDate = BirthDate.builder().year(birthYear).age(LocalDate.now().getYear() - Integer.parseInt(birthYear)).build();
    }

    @AfterEach
    void checkNotMoreInvocation(){
        Mockito.verifyNoMoreInteractions(anprC001RestClientMock, criteriaCodeServiceMock);
    }

    @Test
    void testInvokeOK_requestingBothResidenceAndBirthDate() {
        testInvokeOK(true, true);
    }
    @Test
    void testInvokeOK_requestingResidence() {
        testInvokeOK(true, false);
    }
    @Test
    void testInvokeOK_requestingBirthDate() {
        testInvokeOK(false, true);
    }
    @Test
    void testInvokeOK_noRequests() {
        testInvokeOK(false, false);
    }

    private PdndServicesInvocation buildPdndServicesInvocation(boolean getResidence, boolean getBirthDate) {
        return new PdndServicesInvocation(false, null, getResidence, getBirthDate, false, null);
    }

    private void testInvokeOK(boolean getResidence, boolean getBirthDate) {
        // Given
        boolean expectedAnprInvocation = getResidence || getBirthDate;
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        if(expectedAnprInvocation){
            Mockito.when(anprC001RestClientMock.invoke(FISCAL_CODE_OK,PDND_INITIATIVE_CONFIG)).thenReturn(Mono.just(anprAnswer));
        }

        // When
        PdndServicesInvocation pdndServicesInvocation = buildPdndServicesInvocation(getResidence, getBirthDate);

        Optional<List<OnboardingRejectionReason>> result = service.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG, pdndServicesInvocation, onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());

        Assertions.assertEquals(Collections.emptyList(), result.get());

        Assertions.assertEquals(getResidence? expectedResidence : null, onboardingRequest.getResidence());
        Assertions.assertEquals(getBirthDate? expectedBirthDate : null, onboardingRequest.getBirthDate());
    }

    @Test
    void testInvokeDailyLimitException() {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();
        Mockito.when(anprC001RestClientMock.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG))
                .thenReturn(Mono.error(new PdndServiceTooManyRequestException(new PdndServiceConfig<>(), new RuntimeException("DUMMY"))));

        // When
        Optional<List<OnboardingRejectionReason>> result = service.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, true), onboardingRequest).block();

        // Then
        Assertions.assertEquals(Optional.empty(), result);
        Assertions.assertNull(onboardingRequest.getResidence());
        Assertions.assertNull(onboardingRequest.getBirthDate());
    }

    @Test
    void testInvoke_noResponse() {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();
        Mockito.when(anprC001RestClientMock.invoke(FISCAL_CODE_OK,PDND_INITIATIVE_CONFIG)).thenReturn(Mono.empty());

        // When
        Optional<List<OnboardingRejectionReason>> result = service.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, true), onboardingRequest).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testInvoke_noSubject() {
        anprOkAnswer.setListaSoggetti(null);
        testExtractWhenUnexpectedResponse(0);

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        anprOkAnswer.setListaSoggetti(listaSoggetti);
        testExtractWhenUnexpectedResponse(1);

        listaSoggetti.setDatiSoggetto(Collections.emptyList());
        testExtractWhenUnexpectedResponse(2);
    }

    private void testExtractWhenUnexpectedResponse(int previousCall) {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();
        Mockito.when(anprC001RestClientMock.invoke(FISCAL_CODE_OK,PDND_INITIATIVE_CONFIG)).thenReturn(Mono.just(anprAnswer));

        // When
        Optional<List<OnboardingRejectionReason>> result = service.invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, true), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(List.of(
                buildExpectedResidenceKoRejectionReason(),
                buildExpectedBirthdateKoRejectionReason()
        ), result.get());
        Assertions.assertNull(onboardingRequest.getResidence());
        Assertions.assertNull(onboardingRequest.getBirthDate());

        Mockito.verify(criteriaCodeServiceMock, Mockito.times(previousCall + 1)).getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_RESIDENCE);
        Mockito.verify(criteriaCodeServiceMock, Mockito.times(previousCall + 1)).getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_BIRTHDATE);
    }

    private OnboardingRejectionReason buildExpectedResidenceKoRejectionReason() {
        return new OnboardingRejectionReason(
                OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO,
                OnboardingConstants.REJECTION_REASON_RESIDENCE_KO,
                CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE_AUTH,
                CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE_AUTH_LABEL,
                "Residenza non disponibile"
        );
    }

    private OnboardingRejectionReason buildExpectedBirthdateKoRejectionReason() {
        return new OnboardingRejectionReason(
                OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO,
                OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO,
                CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE_AUTH,
                CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE_AUTH_LABEL,
                "Data di nascita non disponibile"
        );
    }
}