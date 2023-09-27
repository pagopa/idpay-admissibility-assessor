package it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.connector.pdnd.PdndRestClient;
import it.gov.pagopa.admissibility.connector.pdnd.config.PdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.services.rest.BaseRestPdndServiceClient;
import it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.config.AnprC001ServiceConfig;
import it.gov.pagopa.admissibility.connector.pdnd.services.rest.anpr.config.AnprConfig;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RichiestaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoCriteriRicercaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoDatiRichiestaE002DTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class AnprC001RestClientImpl extends BaseRestPdndServiceClient<RichiestaE002DTO, RispostaE002OKDTO> implements AnprC001RestClient {
    private final  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final CustomSequenceGeneratorRepository customSequenceGeneratorRepository;

    protected AnprC001RestClientImpl(
            ObjectMapper objectMapper,
            PdndConfig pdndConfig,
            AnprConfig anprConfig,
            AnprC001ServiceConfig anprC001ServiceConfig,
            AnprSignAlgorithmRetriever jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient,
            WebClient.Builder webClientBuilder,
            HttpClient httpClient,

            CustomSequenceGeneratorRepository customSequenceGeneratorRepository) {
        super(buildDefaultPdndServiceConfig(anprConfig, anprC001ServiceConfig, RispostaE002OKDTO.class), objectMapper, pdndConfig, jwtSignAlgorithmRetrieverService, pdndRestClient, webClientBuilder, httpClient);
        this.customSequenceGeneratorRepository = customSequenceGeneratorRepository;
    }

    @Override
    public Mono<RispostaE002OKDTO> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig) {
        return generateRequest(fiscalCode)
                .flatMap(request -> invokePdndRestService(
                        h -> {},
                        request,
                        pdndInitiativeConfig
                ));
    }

    private Mono<RichiestaE002DTO> generateRequest(String fiscalCode) {
        return customSequenceGeneratorRepository.nextValue(OnboardingConstants.ANPR_E002_INVOKE)
                .map(sequenceValue -> createRequest(fiscalCode, sequenceValue));
    }

    private RichiestaE002DTO createRequest(String fiscalCode, Long sequenceValue) {
        String dateNow = dateFormat.format(new Date());

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