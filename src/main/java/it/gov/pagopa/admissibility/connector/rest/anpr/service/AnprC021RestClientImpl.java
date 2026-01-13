package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprC021ServiceConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprConfig;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.*;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
public class AnprC021RestClientImpl extends BaseAnprRestClientImpl<RichiestaE002DTO, RispostaE002OKDTO, RispostaKODTO> implements AnprC021RestClient{

    protected AnprC021RestClientImpl(
            ObjectMapper objectMapper,
            PdndConfig pdndConfig,
            AnprConfig anprConfig,
            AnprC021ServiceConfig anprC001ServiceConfig,
            AnprSignAlgorithmC021Retriever jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient,
            WebClient.Builder webClientBuilder,
            HttpClient httpClient,
            CustomSequenceGeneratorRepository customSequenceGeneratorRepository) {
        super(objectMapper, pdndConfig, anprConfig, anprC001ServiceConfig, jwtSignAlgorithmRetrieverService, pdndRestClient, webClientBuilder, httpClient, customSequenceGeneratorRepository, RispostaE002OKDTO.class, RispostaKODTO.class);
    }

    @Override
    protected RichiestaE002DTO createRequest(String fiscalCode, Long sequenceValue) {
        String dateNow = getCurrentDate();

        TipoCriteriRicercaE002DTO criteriRicercaE002DTO = new TipoCriteriRicercaE002DTO()
                .codiceFiscale(fiscalCode);

        TipoDatiRichiestaE002DTO datiRichiestaE002DTO = new TipoDatiRichiestaE002DTO()
                .dataRiferimentoRichiesta(dateNow)
                .motivoRichiesta("1")
                .casoUso("C021");

        return new RichiestaE002DTO()
                .idOperazioneClient(String.valueOf(sequenceValue))
                .criteriRicerca(criteriRicercaE002DTO)
                .datiRichiesta(datiRichiestaE002DTO);
    }


}
