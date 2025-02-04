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

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { FamilyDataRetrieverServiceImpl.class })
class FamilyDataRetrieverServiceTest {

    @MockBean
    private AnprC021RestClient anprC021RestClientMock;

    @MockBean private  UserFiscalCodeService userFiscalCodeServiceMock;

    @MockBean private PagoPaAnprPdndConfig pdndInitiativeConfigMock;

    @MockBean private AnprInfoRepository anprInfoRepositoryMock;

    @Autowired private FamilyDataRetrieverServiceImpl familyDataRetrieverService;


    private final OnboardingDTO request = OnboardingDTOFaker.mockInstance(0, "INITIATIVEID");
    private final Family family = new Family("FAMILYID", Set.of(request.getUserId()));


    private final InitiativeConfig initiativeConfig = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .initiativeName("initiative")
            .organizationName("organization")

            .build();

    private final InitiativeConfig initiativeConfigGuidonia = InitiativeConfig.builder()
            .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
            .initiativeName("bonus")
            .organizationName("comune di guidonia montecelio")
            .build();

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

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectNext(Optional.of(familyTest))
                .verifyComplete();
    }

    @Test
    void testRetrieveFamily_Exception_ListaDatiSoggettoEmpty(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();

        eoo20kDTO.setListaSoggetti(null);
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_DatiSoggettoNull(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();
        eoo20kDTO.setListaSoggetti(new TipoListaSoggettiDTO());
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_GeneralitaNull(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();
        TipoListaSoggettiDTO listaSoggettiDTO = new TipoListaSoggettiDTO();
        TipoDatiSoggettiEnteDTO  datiSoggettiEnteDTO = new TipoDatiSoggettiEnteDTO();

        datiSoggettiEnteDTO.setGeneralita(null);
        listaSoggettiDTO.addDatiSoggettoItem(datiSoggettiEnteDTO);
        eoo20kDTO.setListaSoggetti(listaSoggettiDTO);
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_GeneralitaCodFiscaleNull(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();
        TipoListaSoggettiDTO listaSoggettiDTO = new TipoListaSoggettiDTO();
        TipoDatiSoggettiEnteDTO  datiSoggettiEnteDTO = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO generalitaDTO = new TipoGeneralitaDTO();

        datiSoggettiEnteDTO.setGeneralita(generalitaDTO);
        listaSoggettiDTO.addDatiSoggettoItem(datiSoggettiEnteDTO);
        eoo20kDTO.setListaSoggetti(listaSoggettiDTO);
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_Exception_GeneralitaCodFiscaleCodFiscaleNull(){

        String fiscalCode = "fiscalCode";
        String fiscalCodeHashed = "fiscalCodeHashed";
        String idOperazioneANPR =  "idOperazioneANPR";

        RispostaE002OKDTO eoo20kDTO = new RispostaE002OKDTO();
        TipoListaSoggettiDTO listaSoggettiDTO = new TipoListaSoggettiDTO();
        TipoDatiSoggettiEnteDTO  datiSoggettiEnteDTO = new TipoDatiSoggettiEnteDTO();
        TipoGeneralitaDTO generalitaDTO = new TipoGeneralitaDTO();
        TipoCodiceFiscaleDTO codiceFiscaleDTO = new TipoCodiceFiscaleDTO();

        generalitaDTO.setCodiceFiscale(codiceFiscaleDTO);
        datiSoggettiEnteDTO.setGeneralita(generalitaDTO);
        listaSoggettiDTO.addDatiSoggettoItem(datiSoggettiEnteDTO);
        eoo20kDTO.setListaSoggetti(listaSoggettiDTO);
        eoo20kDTO.idOperazioneANPR(idOperazioneANPR);

        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(request.getUserId())).thenReturn(Mono.just(fiscalCode));
        Mockito.when(anprC021RestClientMock.invoke(eq(fiscalCode),any())).thenReturn(Mono.just(eoo20kDTO));
        Mockito.when(userFiscalCodeServiceMock.getUserId(fiscalCode)).thenReturn(Mono.just(fiscalCodeHashed));
        Mockito.when(anprInfoRepositoryMock.save(any())).thenReturn(Mono.justOrEmpty(new AnprInfo()));

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRetrieveFamily_ReturnEmptyResponse(){

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

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null, initiativeConfigGuidonia.getInitiativeName(),initiativeConfigGuidonia.getOrganizationName()))
                .expectComplete()
                .verify();
    }

    @Test
    void testRetrieveFamily_OK_WithChild(){

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
        datiSoggettiEnteDTO.setLegameSoggetto(new TipoLegameSoggettoCompletoDTO().codiceLegame("3"));
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

        StepVerifier.create(familyDataRetrieverService.retrieveFamily(request,null,initiativeConfigGuidonia.getInitiativeName(),initiativeConfigGuidonia.getOrganizationName()))
                .expectNext(Optional.of(familyTest))
                .verifyComplete();
    }

}

