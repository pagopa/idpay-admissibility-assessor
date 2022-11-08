package it.gov.pagopa.admissibility.rest.residence;

import it.gov.pagopa.admissibility.config.WebClientConfig;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.ApiClient;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.api.E002ServiceApi;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ResidenceAssessmentRestClientImpl implements ResidenceAssessmentRestClient{
    private final WebClient.Builder webClientBuilder;
    private final String residenceAssessmentBaseUrl;

    private final String headRequestSenderCode;
    private final String headRequestAddresseeCode;
    private final String headRequestOperationRequest;
    private final String headRequestSendType;

    private final String dataRequestPersonalDetailsRequest;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ResidenceAssessmentRestClientImpl(@Value("${app.c020-residenceAssessment.base-url}") String residenceAssessmentBaseUrl,

                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.connect-timeout-millis}") int residenceAssessmentConnectTimeOutMillis,
                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.response-timeout-millis}") int residenceAssessmentResponseTimeoutMillis,
                                             @Value("${app.c020-residenceAssessment.web-client.timeouts.read-timeout-handler}") int residenceAssessmentReadTimeoutHandlerMillis,

                                             @Value("${app.c020-residenceAssessment.properties.headRequest.senderCode}") String headRequestSenderCode,
                                             @Value("${app.c020-residenceAssessment.properties.headRequest.addresseeCode}") String headRequestAddresseeCode,
                                             @Value("${app.c020-residenceAssessment.properties.headRequest.operationRequest}") String headRequestOperationRequest,
                                             @Value("${app.c020-residenceAssessment.properties.headRequest.sendType}") String headRequestSendType,

                                             @Value("${app.c020-residenceAssessment.properties.dataRequest.personalDetailsRequest}") String dataRequestPersonalDetailsRequest) {

        HttpClient httpClient = WebClientConfig.getHttpClientWithReadTimeoutHandlerConfig(residenceAssessmentConnectTimeOutMillis, residenceAssessmentResponseTimeoutMillis, residenceAssessmentReadTimeoutHandlerMillis);

        webClientBuilder = ApiClient.buildWebClientBuilder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        this.residenceAssessmentBaseUrl = residenceAssessmentBaseUrl;

        this.headRequestSenderCode = headRequestSenderCode;
        this.headRequestAddresseeCode = headRequestAddresseeCode;
        this.headRequestOperationRequest = headRequestOperationRequest;
        this.headRequestSendType = headRequestSendType;
        this.dataRequestPersonalDetailsRequest = dataRequestPersonalDetailsRequest;
    }

    @Override
    public Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode) {
        E002ServiceApi e002ServiceApi = getE002ServiceApi(accessToken);

        return e002ServiceApi.e002(generateRequest(fiscalCode));//TODO define error code for retry
    }

    private E002ServiceApi getE002ServiceApi(String accessToken) {
        WebClient webClient = webClientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer" + accessToken)
                .build();

        ApiClient  newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(residenceAssessmentBaseUrl);
        return new E002ServiceApi(newApiClient);
    }

    private RichiestaE002DTO generateRequest(String fiscalCode) {
        Date dateNow = new Date();
        String dateWithHoursString = new  SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(dateNow);
        TipoTestataRichiestaE000DTO testataRichiestaE000DTO = new TipoTestataRichiestaE000DTO()
                .idOperazioneClient(fiscalCode.concat(dateWithHoursString))
                .codMittente(headRequestSenderCode)
                .codDestinatario(headRequestAddresseeCode)
                .operazioneRichiesta(headRequestOperationRequest)
                .dataOraRichiesta(dateWithHoursString)
                .tipoOperazione("C")
                .tipoInvio(headRequestSendType);

        TipoCriteriRicercaE002DTO criteriRicercaE002DTO = new TipoCriteriRicercaE002DTO()
                .codiceFiscale(fiscalCode);

        String dateWithoutHoursString = new  SimpleDateFormat("yyyy-MM-dd").format(dateNow);
        TipoDatiRichiestaE002DTO datiRichiestaE002DTO = new TipoDatiRichiestaE002DTO()
                .schedaAnagraficaRichiesta(dataRequestPersonalDetailsRequest)
                .dataRiferimentoRichiesta(dateWithoutHoursString)
                .casoUso("C020");

        return new RichiestaE002DTO()
                .testataRichiesta(testataRichiestaE000DTO)
                .criteriRicerca(criteriRicercaE002DTO)
                .datiRichiesta(datiRichiestaE002DTO);
    }
}