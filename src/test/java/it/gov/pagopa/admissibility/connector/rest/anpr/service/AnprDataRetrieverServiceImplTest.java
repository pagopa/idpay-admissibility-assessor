package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseVisitor;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.enums.PdndResponseType;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaKODTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoDatiSoggettiEnteDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoListaSoggettiDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class AnprDataRetrieverServiceImplTest {

    private AnprDataRetrieverServiceImpl service;
    private AnprC001RestClient anprC001RestClient;
    private CriteriaCodeService criteriaCodeService;
    private TipoResidenzaDTO2ResidenceMapper residenceMapper;

    @BeforeEach
    void setUp() {
        anprC001RestClient = mock(AnprC001RestClient.class);
        criteriaCodeService = mock(CriteriaCodeService.class);
        residenceMapper = mock(TipoResidenzaDTO2ResidenceMapper.class);

        service = new AnprDataRetrieverServiceImpl(anprC001RestClient, criteriaCodeService, residenceMapper);
    }

    @Test
    void testInvokeResidence_NotEmptyList() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();

        PdndServicesInvocation invocation1 = new PdndServicesInvocation("isee", true, "threshold");
        when(anprC001RestClient.invoke(fiscalCode, config))
                .thenReturn(Mono.just(new PdndResponseBase<RispostaE002OKDTO, RispostaKODTO>(PdndResponseType.OK) {
                    @Override
                    public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                        return null;
                    }
                }));

        Residence mockResidence = new Residence();
        when(residenceMapper.apply(any())).thenReturn(mockResidence);
        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig();
        when(criteriaCodeService.getCriteriaCodeConfig(any())).thenReturn(criteriaCodeConfig);
        StepVerifier.create(service.invoke(fiscalCode, config, invocation1, onboarding))
                .expectNextCount(1)
                .verifyComplete();

    }

    @Test
    void testInvokeResidence_EmptyList() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();

        PdndServicesInvocation invocation1 = new PdndServicesInvocation( "isse", false, "threshold");

        when(anprC001RestClient.invoke(fiscalCode, config))
                .thenReturn(Mono.just(new PdndResponseBase<RispostaE002OKDTO, RispostaKODTO>(PdndResponseType.OK) {
                    @Override
                    public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                        return null;
                    }
                }));

        Residence mockResidence = new Residence();
        when(residenceMapper.apply(any())).thenReturn(mockResidence);

        StepVerifier.create(service.invoke(fiscalCode, config, invocation1, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

    }

    @Test
    void testInvoke_WhenInvocationNotRequired_ReturnsOptionalEmptyListAndSkipsRestCall() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("residence", false, "threshold");

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

        verifyNoInteractions(anprC001RestClient);
    }

    @Test
    void testInvoke_WhenUnsupportedCode_ReturnsOptionalEmptyListAndSkipsRestCall() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("ISEE", true, "threshold");

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

        verifyNoInteractions(anprC001RestClient);
    }

    @Test
    void testInvoke_WhenTooManyRequests_ReturnsOptionalEmpty() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("BIRTHDATE", true, "threshold");

        PdndServiceConfig<Object, Object> pdndServiceConfig = new PdndServiceConfig<>();
        pdndServiceConfig.setAudience("anpr-service");

        when(anprC001RestClient.invoke(fiscalCode, config))
                .thenReturn(Mono.error(new PdndServiceTooManyRequestException(pdndServiceConfig, new RuntimeException())));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .expectNext(Optional.empty())
                .verifyComplete();
    }

    @Test
    void testInvokeBirthdate_WithValidPersonalInfo_PopulatesBirthDateAndNoRejections() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("BIRTHDATE", true, "threshold");

        RispostaE002OKDTO okdto = mock(RispostaE002OKDTO.class, RETURNS_DEEP_STUBS);
        TipoGeneralitaDTO personalInfo = new TipoGeneralitaDTO();
        personalInfo.setDataNascita("2000-01-01");
        when(okdto.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita())
                .thenReturn(personalInfo);

        when(anprC001RestClient.invoke(fiscalCode, config)).thenReturn(Mono.just(okResponse(okdto)));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

        BirthDate extractedBirthDate = onboarding.getBirthDate();
        org.junit.jupiter.api.Assertions.assertNotNull(extractedBirthDate);
        org.junit.jupiter.api.Assertions.assertEquals("2000", extractedBirthDate.getYear());
        org.junit.jupiter.api.Assertions.assertTrue(extractedBirthDate.getAge() >= 0);
        verify(criteriaCodeService, never()).getCriteriaCodeConfig(any());
    }

    @Test
    void testInvokeBirthdate_WhenPersonalInfoMissing_AddsBirthdateKoRejectionReason() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("BIRTHDATE", true, "threshold");

        RispostaE002OKDTO okdto = new RispostaE002OKDTO();
        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig("birthdate", "ANPR", "Anagrafe", "birthDate");
        when(criteriaCodeService.getCriteriaCodeConfig(eq(OnboardingConstants.CRITERIA_CODE_BIRTHDATE.toLowerCase())))
                .thenReturn(criteriaCodeConfig);
        when(anprC001RestClient.invoke(fiscalCode, config)).thenReturn(Mono.just(okResponse(okdto)));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .assertNext(result -> {
                    org.junit.jupiter.api.Assertions.assertTrue(result.isPresent());
                    List<OnboardingRejectionReason> reasons = result.get();
                    org.junit.jupiter.api.Assertions.assertEquals(1, reasons.size());
                    org.junit.jupiter.api.Assertions.assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO, reasons.get(0).getType());
                    org.junit.jupiter.api.Assertions.assertEquals("ANPR", reasons.get(0).getAuthority());
                    org.junit.jupiter.api.Assertions.assertEquals("Anagrafe", reasons.get(0).getAuthorityLabel());
                })
                .verifyComplete();
    }

    @Test
    void testInvokeResidence_WithValidResidence_PopulatesResidenceAndNoRejections() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("RESIDENCE", true, "threshold");

        TipoResidenzaDTO residenceDto = new TipoResidenzaDTO();
        Residence mappedResidence = new Residence();

        when(residenceMapper.apply(eq(residenceDto))).thenReturn(mappedResidence);
        when(anprC001RestClient.invoke(fiscalCode, config)).thenReturn(Mono.just(okResponse(okResidenceResponse(residenceDto))));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertSame(mappedResidence, onboarding.getResidence());
        verify(residenceMapper).apply(eq(residenceDto));
        verify(criteriaCodeService, never()).getCriteriaCodeConfig(any());
    }

    @Test
    void testInvokeResidence_WhenResidenceMissing_AddsResidenceKoRejectionReason() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("RESIDENCE", true, "threshold");

        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig("residence", "ANPR", "Anagrafe", "residence");
        when(criteriaCodeService.getCriteriaCodeConfig(eq(OnboardingConstants.CRITERIA_CODE_RESIDENCE.toLowerCase())))
                .thenReturn(criteriaCodeConfig);
        when(anprC001RestClient.invoke(fiscalCode, config)).thenReturn(Mono.just(okResponse(new RispostaE002OKDTO())));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .assertNext(result -> {
                    org.junit.jupiter.api.Assertions.assertTrue(result.isPresent());
                    List<OnboardingRejectionReason> reasons = result.get();
                    org.junit.jupiter.api.Assertions.assertEquals(1, reasons.size());
                    org.junit.jupiter.api.Assertions.assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO, reasons.get(0).getType());
                    org.junit.jupiter.api.Assertions.assertEquals("ANPR", reasons.get(0).getAuthority());
                    org.junit.jupiter.api.Assertions.assertEquals("Anagrafe", reasons.get(0).getAuthorityLabel());
                })
                .verifyComplete();

        verify(residenceMapper, never()).apply(any());
    }

    @Test
    void testInvokeResidence_WhenKoResponse_AddsResidenceKoRejectionReason() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();
        PdndServicesInvocation invocation = new PdndServicesInvocation("RESIDENCE", true, "threshold");

        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig("residence", "ANPR", "Anagrafe", "residence");
        when(criteriaCodeService.getCriteriaCodeConfig(eq(OnboardingConstants.CRITERIA_CODE_RESIDENCE.toLowerCase())))
                .thenReturn(criteriaCodeConfig);
        when(anprC001RestClient.invoke(fiscalCode, config)).thenReturn(Mono.just(koResponse(new RispostaKODTO())));

        StepVerifier.create(service.invoke(fiscalCode, config, invocation, onboarding))
                .assertNext(result -> {
                    org.junit.jupiter.api.Assertions.assertTrue(result.isPresent());
                    List<OnboardingRejectionReason> reasons = result.get();
                    org.junit.jupiter.api.Assertions.assertEquals(1, reasons.size());
                    org.junit.jupiter.api.Assertions.assertEquals(OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO, reasons.get(0).getType());
                })
                .verifyComplete();

        verify(residenceMapper, never()).apply(any());
    }

    private PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> okResponse(RispostaE002OKDTO okdto) {
        return new PdndResponseBase<>(PdndResponseType.OK) {
            @Override
            public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                return visitor.onOk(okdto);
            }
        };
    }

    private PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> koResponse(RispostaKODTO kodto) {
        return new PdndResponseBase<>(PdndResponseType.KO) {
            @Override
            public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                return visitor.onKo(kodto);
            }
        };
    }

    private RispostaE002OKDTO okResidenceResponse(TipoResidenzaDTO residenceDto) {
        TipoDatiSoggettiEnteDTO datiSoggetto = new TipoDatiSoggettiEnteDTO();
        datiSoggetto.setResidenza(List.of(residenceDto));

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        listaSoggetti.addDatiSoggettoItem(datiSoggetto);

        RispostaE002OKDTO response = new RispostaE002OKDTO();
        response.setListaSoggetti(listaSoggetti);
        return response;
    }

}