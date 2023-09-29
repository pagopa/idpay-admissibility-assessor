package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClientImpl=WARN",
})
@DirtiesContext
public class AnprC001RestClientImplIntegrationTest extends BaseIntegrationTest {

    public static final String FISCAL_CODE = "fiscalCode";
    public static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG = new PdndInitiativeConfig(
            "CLIENTID",
            "KID",
            "PURPOSEID"
    );

    @SpyBean
    private AnprC001RestClient anprC001RestClient;

    @SpyBean
    private ObjectMapper objectMapper;

    @Test
    void getResidenceAssessment(){
        // Given
        String fiscalCode = "FISCAL_CODE";
        PdndInitiativeConfig pdndInitiativeConfig = new PdndInitiativeConfig(
                "CLIENTID",
                "KID",
                "PURPOSEID"
        );

        // When
        RispostaE002OKDTO result = anprC001RestClient.invoke(fiscalCode, pdndInitiativeConfig).block();

        // Then
        Assertions.assertEquals(buildExpectedResponse(), result);

//        Assertions.assertNotNull(result.getListaSoggetti());
//        Assertions.assertFalse(CollectionUtils.isEmpty(result.getListaSoggetti().getDatiSoggetto()));

//        TipoDatiSoggettiEnteDTO expected = new TipoDatiSoggettiEnteDTO();
//
//        expected.setGeneralita(getExpectedGeneralita());
//        expected.setResidenza(List.of(getExpectedResidenza()));

//        Assertions.assertEquals(List.of(expected), result.getListaSoggetti().getDatiSoggetto());
    }

//    private TipoGeneralitaDTO getExpectedGeneralita() {
//        TipoCodiceFiscaleDTO codiceFiscale = new TipoCodiceFiscaleDTO();
//        codiceFiscale.setCodFiscale("CF_OK");
//
//        TipoGeneralitaDTO out = new TipoGeneralitaDTO();
//        out.setCodiceFiscale(codiceFiscale);
//        out.setNome("name");
//        out.setCognome("lastName");
//        out.setDataNascita("1970-01-01");
//        out.setSenzaGiornoMese("1970");
//
//        return out;
//    }
//
//    private TipoResidenzaDTO getExpectedResidenza() {
//        TipoComuneDTO comune = new TipoComuneDTO();
//        comune.setNomeComune("Milano");
//        comune.setSiglaProvinciaIstat("MI");
//
//        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
//        indirizzo.setCap("20143");
//        indirizzo.setComune(comune);
//
//        TipoResidenzaDTO out = new TipoResidenzaDTO();
//        out.setIndirizzo(indirizzo);
//        return out;
//    }

    @Test
    @SneakyThrows
    void objectMapperException(){
        // Given
        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);

        // When
        try {
            anprC001RestClient.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG).block();
        }catch (Exception e){
            // Then
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
    }

    public static RispostaE002OKDTO buildExpectedResponse() {

        return new RispostaE002OKDTO().listaSoggetti(buildListaSoggetti());
    }

    private static TipoListaSoggettiDTO buildListaSoggetti() {
        TipoGeneralitaDTO generalita = new TipoGeneralitaDTO();
        generalita.setDataNascita("2001-02-04");
        generalita.setSenzaGiornoMese("2001");

        TipoComuneDTO comune = new TipoComuneDTO();
        comune.setNomeComune("Milano");
        comune.setSiglaProvinciaIstat("MI");

        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
        indirizzo.setCap("20143");
        indirizzo.setComune(comune);

        TipoResidenzaDTO residenza = new TipoResidenzaDTO();
        residenza.setIndirizzo(indirizzo);

        TipoDatiSoggettiEnteDTO datiSoggetto = new TipoDatiSoggettiEnteDTO();
        datiSoggetto.setGeneralita(generalita);
        datiSoggetto.setResidenza(List.of(residenza));

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        listaSoggetti.setDatiSoggetto(List.of(datiSoggetto));

        return listaSoggetti;
    }
}