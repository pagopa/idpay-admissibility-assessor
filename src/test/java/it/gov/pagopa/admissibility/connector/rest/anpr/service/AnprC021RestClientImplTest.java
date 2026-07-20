package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprC021ServiceConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprConfig;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RichiestaE002DTO;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceProviderConfig;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnprC021RestClientImplTest {

    private AnprC021RestClientImpl anprC021RestClient;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        PdndConfig pdndConfig = new PdndConfig();
        pdndConfig.setAudience("pdnd");
        AnprConfig anprConfig = new AnprConfig();
        BasePdndServiceProviderConfig.HttpsConfig httpsConfig = new BasePdndServiceProviderConfig.HttpsConfig();
        httpsConfig.setEnabled(false);
        httpsConfig.setMutualAuthEnabled(false);
        anprConfig.setHttpsConfig(httpsConfig);
        AnprC021ServiceConfig serviceConfig = Mockito.mock(AnprC021ServiceConfig.class);
        PdndRestClient pdndRestClient = Mockito.mock(PdndRestClient.class);
        WebClient.Builder webClientBuilder = WebClient.builder();
        HttpClient httpClient = HttpClient.create();
        CustomSequenceGeneratorRepository sequenceRepo = Mockito.mock(CustomSequenceGeneratorRepository.class);
        AnprSignAlgorithmC021Retriever fakeJwtRetriever = Mockito.mock(AnprSignAlgorithmC021Retriever.class);
        anprC021RestClient = new AnprC021RestClientImpl(
                objectMapper,
                pdndConfig,
                anprConfig,
                serviceConfig,
                fakeJwtRetriever,
                pdndRestClient,
                webClientBuilder,
                httpClient,
                sequenceRepo
        );
    }

    @Test
    void testCreateRequest() {
        String fiscalCode = "RSSMRA85M01H501Z";
        Long sequenceValue = 123L;

        RichiestaE002DTO request = anprC021RestClient.createRequest(fiscalCode, sequenceValue);

        assertEquals(fiscalCode, request.getCriteriRicerca().getCodiceFiscale());
        assertEquals(String.valueOf(sequenceValue), request.getIdOperazioneClient());
        assertEquals("1", request.getDatiRichiesta().getMotivoRichiesta());
        assertEquals("C021", request.getDatiRichiesta().getCasoUso());
    }
}