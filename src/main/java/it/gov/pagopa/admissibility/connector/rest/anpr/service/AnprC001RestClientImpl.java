package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprC001ServiceConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprConfig;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RichiestaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoCriteriRicercaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoDatiRichiestaE002DTO;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
@Service
public class AnprC001RestClientImpl extends BaseAnprRestClientImpl<RichiestaE002DTO, RispostaE002OKDTO> implements AnprC001RestClient{

    protected AnprC001RestClientImpl(
            ObjectMapper objectMapper,
            PdndConfig pdndConfig,
            AnprConfig anprConfig,
            AnprC001ServiceConfig anprC001ServiceConfig,
            AnprSignAlgorithmC001Retriever jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient,
            WebClient.Builder webClientBuilder,
            HttpClient httpClient,
            CustomSequenceGeneratorRepository customSequenceGeneratorRepository) {
        super(objectMapper, pdndConfig, anprConfig, anprC001ServiceConfig, jwtSignAlgorithmRetrieverService, pdndRestClient, webClientBuilder, httpClient, customSequenceGeneratorRepository, RispostaE002OKDTO.class);
    }

    @Override
    protected RichiestaE002DTO createRequest(String fiscalCode, Long sequenceValue) {
        String dateNow = getCurrentDate();

        TipoCriteriRicercaE002DTO criteriRicercaE002DTO = new TipoCriteriRicercaE002DTO()
                .codiceFiscale(fiscalCode);

        TipoDatiRichiestaE002DTO datiRichiestaE002DTO = new TipoDatiRichiestaE002DTO()
                .dataRiferimentoRichiesta(dateNow)
                .motivoRichiesta("1")
                .casoUso("C001");

        return new RichiestaE002DTO()
                .idOperazioneClient(String.valueOf(sequenceValue))
                .criteriRicerca(criteriRicercaE002DTO)
                .datiRichiesta(datiRichiestaE002DTO);
    }


}
