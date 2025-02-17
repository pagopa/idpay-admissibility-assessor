package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.repository.AnprInfoRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.*;
import it.gov.pagopa.admissibility.model.AnprInfo;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.ExternalConstants;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { FamilyDataRetrieverServiceImpl.class })
class FamilyDataRetrieverServiceTest {


    @MockBean
    private AnprC021RestClient anprC021RestClientMock;

    @MockBean
    private UserFiscalCodeService userFiscalCodeService;

    @MockBean
    private PagoPaAnprPdndConfig pdndInitiativeConfigMock;

    @MockBean
    private AnprInfoRepository anprInfoRepositoryMock;

    @MockBean
    private ExternalConstants externalConstants;

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

    private static final InitiativeConfig INITIATIVE_CONFIG_GUIDONIA = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .initiativeName("bonus")
            .organizationName("comune di guidonia montecelio")
            .build();

    private static final Family EXPECTED_FAMILY = Family.builder()
            .familyId(ID_OPERAZIONE_ANPR)
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
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));
        List<String> entityEnabledList = new ArrayList<>();
        entityEnabledList.add("COMUNE DI GUIDONIA MONTECELIO");
        entityEnabledList.add("COMUNE DI ROMA");
        Mockito.when(externalConstants.getEntityEnabledList()).thenReturn(entityEnabledList);
    }

    @Test
    void testRetrieveFamily_OK_WithOutChild() {


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

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG.getInitiativeName(), INITIATIVE_CONFIG.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_ReturnEmptyResponse() {


        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, null, null)));

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG_GUIDONIA.getInitiativeName(), INITIATIVE_CONFIG_GUIDONIA.getOrganizationName()))
                .expectComplete()
                .verify();
    }

    @Test
    void testRetrieveFamily_OK_WithChild() {
        RispostaE002OKDTO response = createResponse(ID_OPERAZIONE_ANPR,
                List.of(createDatiSoggetti(FISCAL_CODE, "2007-12-31", "3"), createDatiSoggetti(FISCAL_CODE, "2007-12-31","1"))
        );

        mockDependencies(FISCAL_CODE, FISCAL_CODE_HASHED, response);

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(REQUEST, null, INITIATIVE_CONFIG_GUIDONIA.getInitiativeName(), INITIATIVE_CONFIG_GUIDONIA.getOrganizationName()))
                .expectNext(Optional.of(EXPECTED_FAMILY))
                .verifyComplete();
    }
}
