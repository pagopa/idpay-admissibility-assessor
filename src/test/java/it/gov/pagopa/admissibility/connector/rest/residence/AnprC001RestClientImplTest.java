package it.gov.pagopa.admissibility.connector.rest.residence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service.AnprC001RestClient;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.CollectionUtils;

import java.util.List;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service.AnprC001RestClientImpl=WARN",
})
@DirtiesContext
class AnprC001RestClientImplTest extends BaseIntegrationTest {

    @SpyBean
    private AnprC001RestClient anprC001RestClient;

    @SpyBean
    private ObjectMapper objectMapper;

    @Test
    void getResidenceAssessment(){
        // Given
        String accessToken = "VALID_ACCESS_TOKEN_1";
        String fiscalCode = "FISCAL_CODE";

        AgidJwtTokenPayload agidTokenPayload = AgidJwtTokenPayload.builder()
                .iss("ISS")
                .sub("SUB").build();

        // When
        RispostaE002OKDTO result = anprC001RestClient.invoke(accessToken, fiscalCode,agidTokenPayload).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getListaSoggetti());
        Assertions.assertFalse(CollectionUtils.isEmpty(result.getListaSoggetti().getDatiSoggetto()));

        TipoDatiSoggettiEnteDTO expected = new TipoDatiSoggettiEnteDTO();

        expected.setGeneralita(getExpectedGeneralita());
        expected.setResidenza(List.of(getExpectedResidenza()));

        Assertions.assertEquals(List.of(expected), result.getListaSoggetti().getDatiSoggetto());
    }

    private TipoGeneralitaDTO getExpectedGeneralita() {
        TipoCodiceFiscaleDTO codiceFiscale = new TipoCodiceFiscaleDTO();
        codiceFiscale.setCodFiscale("CF_OK");

        TipoGeneralitaDTO out = new TipoGeneralitaDTO();
        out.setCodiceFiscale(codiceFiscale);
        out.setNome("name");
        out.setCognome("lastName");
        out.setDataNascita("1970-01-01");
        out.setSenzaGiornoMese("1970");

        return out;
    }

    private TipoResidenzaDTO getExpectedResidenza() {
        TipoComuneDTO comune = new TipoComuneDTO();
        comune.setNomeComune("Milano");
        comune.setSiglaProvinciaIstat("MI");

        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
        indirizzo.setCap("20143");
        indirizzo.setComune(comune);

        TipoResidenzaDTO out = new TipoResidenzaDTO();
        out.setIndirizzo(indirizzo);
        return out;
    }

    @Test
    @SneakyThrows
    void objectMapperException(){
        // Given
        AgidJwtTokenPayload agidTokenPayload = AgidJwtTokenPayload.builder()
                .iss("ISS")
                .sub("SUB").build();
        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);

        // When
        try {
            anprC001RestClient.invoke("VALID_TOKEN", "FISCAL_CODE",agidTokenPayload).block();
        }catch (Exception e){
            // Then
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
    }
}