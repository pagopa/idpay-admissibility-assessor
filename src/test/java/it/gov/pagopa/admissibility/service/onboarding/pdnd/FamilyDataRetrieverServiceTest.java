package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.exception.AnprAnomalyErrorCodeException;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "app.anpr.c021-servizio-accertamento-stato-famiglia.ko-anomaly-code-list=EN122"
})
@ContextConfiguration(classes = { FamilyDataRetrieverServiceImpl.class })
class FamilyDataRetrieverServiceTest {

    @MockBean
    private AnprC021RestClient anprC021RestClientMock;

    @MockBean
    private UserFiscalCodeService userFiscalCodeService;

    @MockBean
    private PagoPaAnprPdndConfig pdndInitiativeConfigMock;

    @MockBean
    private AuditUtilities auditUtilities;

    @Autowired
    private FamilyDataRetrieverServiceImpl familyDataRetrieverService;

    private static final String ID_OPERAZIONE_ANPR = "ID_OPERAZIONE_ANPR";

    private static final String FISCAL_CODE = "FISCAL_CODE";

    private static final String FISCAL_CODE_HASHED = "FISCAL_CODE_HASHED";

    private static final OnboardingDTO REQUEST = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");

    private static final InitiativeConfig INITIATIVE_CONFIG = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .initiativeName("initiative")
            .organizationName("organization")
            .build();

    private static final Family EXPECTED_FAMILY = Family.builder()
            .familyId("3b3be3e7704b96cb177b9076c92b0c31c71a0163220f2f06f3370fec41d21c7b")
            .memberIds(Set.of(FISCAL_CODE_HASHED))
            .build();

    private RispostaE002OKDTO createResponse(String idOperazioneAnpr, List<TipoDatiSoggettiEnteDTO> datiSoggetti) {
        TipoListaSoggettiDTO listaSoggettiDTO = new TipoListaSoggettiDTO();
        if (datiSoggetti != null) {
            datiSoggetti.forEach(listaSoggettiDTO::addDatiSoggettoItem);
        }

        RispostaE002OKDTO response = new RispostaE002OKDTO();
        response.setListaSoggetti(listaSoggettiDTO);
        response.setIdOperazioneANPR(idOperazioneAnpr);
        return response;
    }

    private TipoDatiSoggettiEnteDTO createDatiSoggetti(String fiscalCode, String dateOfBirth, String legame) {
        TipoCodiceFiscaleDTO codiceFiscaleDTO = new TipoCodiceFiscaleDTO();
        codiceFiscaleDTO.setCodFiscale(fiscalCode);

        TipoGeneralitaDTO generalitaDTO = new TipoGeneralitaDTO();
        generalitaDTO.setCodiceFiscale(codiceFiscaleDTO);
        if (dateOfBirth != null) {
            generalitaDTO.setDataNascita(dateOfBirth);
        }

        TipoDatiSoggettiEnteDTO datiSoggettiEnteDTO = new TipoDatiSoggettiEnteDTO();
        datiSoggettiEnteDTO.setGeneralita(generalitaDTO);
        if (legame != null) {
            datiSoggettiEnteDTO.setLegameSoggetto(new TipoLegameSoggettoCompletoDTO().codiceLegame(legame));
        }

        return datiSoggettiEnteDTO;
    }

    private void mockDependencies(String fiscalCode, String fiscalCodeHashed, RispostaE002OKDTO response) {
        Mockito.when(userFiscalCodeService.getUserFiscalCode(REQUEST.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode), any())).thenReturn(Mono.just(response));
        Mockito.when(userFiscalCodeService.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
    }

    @Test
    void testRetrieveFamily_OK_WithFamily() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, null, null)));

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectNext(Optional.of(EXPECTED_FAMILY))
                .verifyComplete();
    }

    @Test
    void testRetrieveFamily_Exception_ListaDatiSoggettoEmpty() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR, null);
        response.setListaSoggetti(null);
        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_DatiSoggettoNull() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR, List.of());
        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_GeneralitaNull() {
        TipoDatiSoggettiEnteDTO datiSoggetti = new TipoDatiSoggettiEnteDTO();
        datiSoggetti.setGeneralita(null);

        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR, List.of(datiSoggetti));
        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        Mono<Optional<Family>> result = familyDataRetrieverService
                .retrieveFamily(REQUEST, null,
                        INITIATIVE_CONFIG.getInitiativeName(),
                        INITIATIVE_CONFIG.getOrganizationName());
        Assertions.assertThrows(RuntimeException.class, result::block);
    }

    @Test
    void testProcessDatiSoggetto_Exception_CodiceFiscaleNull() {
        TipoDatiSoggettiEnteDTO datiSoggetti = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO tipoGeneralita = new TipoGeneralitaDTO();
        tipoGeneralita.setCodiceFiscale(null);
        datiSoggetti.setGeneralita(tipoGeneralita);

        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR, List.of(datiSoggetti));
        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        Mono<Optional<Family>> result = familyDataRetrieverService
                .retrieveFamily(REQUEST, null,
                        INITIATIVE_CONFIG.getInitiativeName(),
                        INITIATIVE_CONFIG.getOrganizationName());
        Assertions.assertThrows(RuntimeException.class, result::block);
    }

    @Test
    void testProcessDatiSoggetto_Exception_CodFiscaleNull() {
        TipoDatiSoggettiEnteDTO datiSoggetti = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO tipoGeneralita = new TipoGeneralitaDTO();
        TipoCodiceFiscaleDTO tipoCodiceFiscaleDTO = new TipoCodiceFiscaleDTO();
        tipoCodiceFiscaleDTO.setCodFiscale(null);
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscaleDTO);
        datiSoggetti.setGeneralita(tipoGeneralita);

        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR, List.of(datiSoggetti));
        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        Mono<Optional<Family>> result = familyDataRetrieverService
                .retrieveFamily(REQUEST, null,
                        INITIATIVE_CONFIG.getInitiativeName(),
                        INITIATIVE_CONFIG.getOrganizationName());

        Assertions.assertThrows(RuntimeException.class, result::block);
    }

    @Test
    void testRetrieveFamily_OK_familyWithMemberUnder18() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, null, null)));

        TipoDatiSoggettiEnteDTO datiSoggetti = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO tipoGeneralita = new TipoGeneralitaDTO();
        TipoCodiceFiscaleDTO tipoCodiceFiscaleDTO = new TipoCodiceFiscaleDTO();
        tipoCodiceFiscaleDTO.setCodFiscale("CF_UNDER_18");
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscaleDTO);
        LocalDate lastYear = LocalDate.now().minusYears(1);
        tipoGeneralita.setDataNascita(lastYear.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        datiSoggetti.setGeneralita(tipoGeneralita);

        assert response.getListaSoggetti() != null;
        response.getListaSoggetti().addDatiSoggettoItem(datiSoggetti);

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectNext(Optional.of(EXPECTED_FAMILY))
                .verifyComplete();
    }

    @Test
    void testRetrieveFamily_KoAnomaly() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, null, null)));

        TipoErroriAnomaliaDTO anomalyError = new TipoErroriAnomaliaDTO();
        anomalyError.codiceErroreAnomalia("EN122");
        anomalyError.testoErroreAnomalia("Soggetto non registrato in ANPR");
        anomalyError.tipoErroreAnomalia("E");

        response.listaAnomalie(List.of(anomalyError));

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        Mono<Optional<Family>> monoResult = familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName());

        Assertions.assertThrows(AnprAnomalyErrorCodeException.class, monoResult::block);
    }

    @Test
    void testRetrieveFamily_NoKoAnomaly() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, null, null)));

        TipoErroriAnomaliaDTO anomalyError = new TipoErroriAnomaliaDTO();
        anomalyError.codiceErroreAnomalia("EN124");
        anomalyError.testoErroreAnomalia("Soggetto non registrato in ANPR");
        anomalyError.tipoErroreAnomalia("E");

        response.listaAnomalie(List.of(anomalyError));

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectNext(Optional.of(EXPECTED_FAMILY))
                .verifyComplete();
    }

}
