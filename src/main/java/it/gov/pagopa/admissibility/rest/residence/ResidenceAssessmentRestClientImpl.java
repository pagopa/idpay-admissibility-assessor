package it.gov.pagopa.admissibility.rest.residence;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.ApiClient;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.api.E002ServiceApi;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RichiestaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoCriteriRicercaE002DTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoDatiRichiestaE002DTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class ResidenceAssessmentRestClientImpl implements ResidenceAssessmentRestClient{
    private E002ServiceApi e002ServiceApi;

    public ResidenceAssessmentRestClientImpl(@Value("${app.c020-residenceAssessment.base-url}") String residenceAssesmentBaseUrl,

                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.connect-timeout-millis}") int residenceAssessmentConnectTimeOutMillis,
                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.response-timeout-millis}") int residenceAssessmentResponseTimeoutMillis,
                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.read-timeout-handler}") int residenceAssessmentReadTimeoutHandlerMillis) {
        HttpClient httpClient = HttpClient.create() //TODO riuse method
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, residenceAssessmentConnectTimeOutMillis)
                .responseTimeout(Duration.ofMillis(residenceAssessmentResponseTimeoutMillis))
                .doOnConnected( connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(residenceAssessmentReadTimeoutHandlerMillis, TimeUnit.MILLISECONDS)));

        WebClient webClient = ApiClient.buildWebClientBuilder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        ApiClient  newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(residenceAssesmentBaseUrl);
        e002ServiceApi = new E002ServiceApi(newApiClient);
    }

    @Override
    public Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode) {
        //TODO AccessToken
        return e002ServiceApi.e002(generateRequest(fiscalCode));//TODO define error code for retry
    }

    private RichiestaE002DTO generateRequest(String fiscalCode) {
        //TODO other 2 fields present in RichiestaE002DTO TipoTestataRichiestaE000DTO, TipoVerificaE002DTO
        TipoCriteriRicercaE002DTO criteriRicercaE002DTO = new TipoCriteriRicercaE002DTO();
        criteriRicercaE002DTO.setCodiceFiscale(fiscalCode);

        TipoDatiRichiestaE002DTO datiRichiestaE002DTO = new TipoDatiRichiestaE002DTO();
        datiRichiestaE002DTO.setCasoUso("C020");
        datiRichiestaE002DTO.setDataRiferimentoRichiesta(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        // TODO numero di riferimento della pratica

        RichiestaE002DTO richiestaE002DTO = new RichiestaE002DTO();
        richiestaE002DTO.setCriteriRicerca(criteriRicercaE002DTO);
        richiestaE002DTO.setDatiRichiesta(datiRichiestaE002DTO);

        return richiestaE002DTO;
    }
}