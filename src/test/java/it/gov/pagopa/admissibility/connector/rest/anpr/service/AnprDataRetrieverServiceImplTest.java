package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndOkResponse;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper =
            new TipoResidenzaDTO2ResidenceMapper();

    private AnprDataRetrieverService service;

    private RispostaE002OKDTO anprOkAnswer;
    private PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> anprAnswer;
    private Residence expectedResidence;
    private BirthDate expectedBirthDate;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);

        service = new AnprDataRetrieverServiceImpl(
                anprC001RestClientMock,
                criteriaCodeServiceMock,
                residenceMapper
        );

        anprOkAnswer =
                AnprC001RestClientImplIntegrationTest.buildExpectedResponse();

        TipoDatiSoggettiEnteDTO subject =
                anprOkAnswer.getListaSoggetti().getDatiSoggetto().get(0);

        TipoIndirizzoDTO address = subject.getResidenza().get(0).getIndirizzo();
        expectedResidence = Residence.builder()
                .cityCouncil(address.getComune().getNomeComune())
                .city(address.getComune().getNomeComune())
                .province(address.getComune().getSiglaProvinciaIstat())
                .postalCode(address.getCap())
                .build();

        anprAnswer = new PdndOkResponse<>(anprOkAnswer);

        String birthYear =
                subject.getGeneralita().getDataNascita().substring(0, 4);

        expectedBirthDate = BirthDate.builder()
                .year(birthYear)
                .age(LocalDate.now().getYear() - Integer.parseInt(birthYear))
                .build();
    }

    @Test
    void invokeResidence_ok() {

        Mockito.when(anprC001RestClientMock
                        .invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG))
                .thenReturn(Mono.just(anprAnswer));

        OnboardingDTO onboarding = new OnboardingDTO();

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE_OK,
                        PDND_INITIATIVE_CONFIG,
                        invocation(OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                        onboarding
                ).block();

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().isEmpty());
        Assertions.assertEquals(expectedResidence, onboarding.getResidence());
        Assertions.assertNull(onboarding.getBirthDate());
    }

    @Test
    void invokeBirthdate_ok() {

        Mockito.when(anprC001RestClientMock
                        .invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG))
                .thenReturn(Mono.just(anprAnswer));

        OnboardingDTO onboarding = new OnboardingDTO();

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE_OK,
                        PDND_INITIATIVE_CONFIG,
                        invocation(OnboardingConstants.CRITERIA_CODE_BIRTHDATE),
                        onboarding
                ).block();

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().isEmpty());
        Assertions.assertEquals(expectedBirthDate, onboarding.getBirthDate());
        Assertions.assertNull(onboarding.getResidence());
    }

    @Test
    void invoke_noSubject_residenceKo_then_birthdateKo() {

        anprOkAnswer.setListaSoggetti(null);

        Mockito.when(anprC001RestClientMock
                        .invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG))
                .thenReturn(Mono.just(anprAnswer));

        // RESIDENCE
        OnboardingDTO onboarding1 = new OnboardingDTO();
        Optional<List<OnboardingRejectionReason>> res1 =
                service.invoke(
                        FISCAL_CODE_OK,
                        PDND_INITIATIVE_CONFIG,
                        invocation(OnboardingConstants.CRITERIA_CODE_RESIDENCE.toLowerCase()),
                        onboarding1
                ).block();

        Assertions.assertEquals(
                List.of(buildExpectedResidenceKoRejectionReason()),
                res1.get()
        );

        // BIRTHDATE
        OnboardingDTO onboarding2 = new OnboardingDTO();
        Optional<List<OnboardingRejectionReason>> res2 =
                service.invoke(
                        FISCAL_CODE_OK,
                        PDND_INITIATIVE_CONFIG,
                        invocation(OnboardingConstants.CRITERIA_CODE_BIRTHDATE.toLowerCase()),
                        onboarding2
                ).block();

        Assertions.assertEquals(
                List.of(buildExpectedBirthdateKoRejectionReason()),
                res2.get()
        );
    }

    @Test
    void invokeDailyLimitException() {

        Mockito.when(anprC001RestClientMock
                        .invoke(FISCAL_CODE_OK, PDND_INITIATIVE_CONFIG))
                .thenReturn(
                        Mono.error(new PdndServiceTooManyRequestException(
                                new PdndServiceConfig<>(),
                                new RuntimeException("DUMMY")
                        ))
                );

        OnboardingDTO onboarding = new OnboardingDTO();
        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE_OK,
                        PDND_INITIATIVE_CONFIG,
                        invocation(OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                        onboarding
                ).block();

        Assertions.assertEquals(Optional.empty(), result);
        Assertions.assertNull(onboarding.getResidence());
        Assertions.assertNull(onboarding.getBirthDate());
    }


    private PdndServicesInvocation invocation(String code) {
        return new PdndServicesInvocation(code, true, null);
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