package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.connector.rest.anpr.residence.ResidenceAssessmentRestClient;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoListaSoggettiDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ResidenceDataRetrieverServiceImplTest {
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String FISCAL_CODE = "fiscalCode";

    @Mock
    private ResidenceAssessmentRestClient residenceAssessmentRestClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private RispostaE002OKDTO anprAnswer;
    private Residence expectedResidence;
    private BirthDate expectedBirthDate;

    private OnboardingDTO onboardingRequest;
    private AgidJwtTokenPayload agidJwtTokenPayload;

    private ResidenceDataRetrieverService residenceDataRetrieverService;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        residenceDataRetrieverService = new ResidenceDataRetrieverServiceImpl(residenceAssessmentRestClientMock, criteriaCodeServiceMock, residenceMapper);

        anprAnswer = PdndInvocationsTestUtils.buildAnprResponse();
        expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        expectedBirthDate = BirthDate.builder().year("2001").age(LocalDate.now().getYear() - 2001).build();

        onboardingRequest = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .build();

        agidJwtTokenPayload = AgidJwtTokenPayload.builder()
                .iss("ISS")
                .sub("SUB").build();
    }

    @Test
    void testInvokeOK() {
        // Given
        Mockito.when(residenceAssessmentRestClientMock.getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE,agidJwtTokenPayload)).thenReturn(Mono.just(anprAnswer));

        // When
        Optional<RispostaE002OKDTO> result = residenceDataRetrieverService.invoke(ACCESS_TOKEN, FISCAL_CODE,agidJwtTokenPayload).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(anprAnswer, result.get());
    }

    @Test
    void testInvokeDailyLimitException() {
        // Given
        Mockito.when(residenceAssessmentRestClientMock.getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE,agidJwtTokenPayload)).thenReturn(Mono.error(AnprDailyRequestLimitException::new));

        // When
        Optional<RispostaE002OKDTO> result = residenceDataRetrieverService.invoke(ACCESS_TOKEN, FISCAL_CODE,agidJwtTokenPayload).block();

        // Then
        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void testExtract() {
        // When
        List<OnboardingRejectionReason> result = residenceDataRetrieverService.extract(anprAnswer, true, true, onboardingRequest);

        // Then
        Assertions.assertEquals(Collections.emptyList(), result);
        Assertions.assertEquals(expectedResidence, onboardingRequest.getResidence());
        Assertions.assertEquals(expectedBirthDate, onboardingRequest.getBirthDate());
    }

//region desc=TestWhenUnexpectedResponse
    @Test
    void testExtractWhenResponseNull() {
        anprAnswer=null;
        testExtractWhenUnexpectedResponse();
    }

    @Test
    void testExtractWhenNoSubjects() {
        anprAnswer.setListaSoggetti(null);
        testExtractWhenUnexpectedResponse();

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        anprAnswer.setListaSoggetti(listaSoggetti);
        testExtractWhenUnexpectedResponse();

        listaSoggetti.setDatiSoggetto(Collections.emptyList());
        testExtractWhenUnexpectedResponse();
    }

    private void testExtractWhenUnexpectedResponse() {
        // When
        List<OnboardingRejectionReason> result = residenceDataRetrieverService.extract(anprAnswer, true, true, onboardingRequest);

        // Then
        Assertions.assertEquals(List.of(
                buildExpectedResidenceKoRejectionReason(),
                buildExpectedBirthdateKoRejectionReason()
        ), result);
        Assertions.assertNull(onboardingRequest.getResidence());
        Assertions.assertNull(onboardingRequest.getBirthDate());
    }
//endregion

    @Test
    void testExtractWhenNoResidence() {
        //Given

        anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).setResidenza(null);

        // When
        List<OnboardingRejectionReason> result = residenceDataRetrieverService.extract(anprAnswer, true, true, onboardingRequest);

        // Then
        Assertions.assertEquals(List.of(
                buildExpectedResidenceKoRejectionReason()
        ), result);
        Assertions.assertNull(onboardingRequest.getResidence());

        Assertions.assertEquals(expectedBirthDate, onboardingRequest.getBirthDate());
    }

    @Test
    void testExtractWhenNoBirthDate() {
        anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).setGeneralita(null);
        testExtractWhenNoBirthDateInner();

        anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).setGeneralita(new TipoGeneralitaDTO());
        testExtractWhenNoBirthDateInner();
    }

    private void testExtractWhenNoBirthDateInner() {
        // When
        List<OnboardingRejectionReason> result = residenceDataRetrieverService.extract(anprAnswer, true, true, onboardingRequest);

        // Then
        Assertions.assertEquals(List.of(
                buildExpectedBirthdateKoRejectionReason()
        ), result);
        Assertions.assertNull(onboardingRequest.getBirthDate());

        Assertions.assertEquals(expectedResidence, onboardingRequest.getResidence());
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