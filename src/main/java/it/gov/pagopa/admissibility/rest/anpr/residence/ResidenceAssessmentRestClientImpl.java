package it.gov.pagopa.admissibility.rest.anpr.residence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.rest.anpr.AgidJwtSignature;
import it.gov.pagopa.admissibility.rest.anpr.AnprWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class ResidenceAssessmentRestClientImpl implements ResidenceAssessmentRestClient{
    private final WebClient webClient;
    private final AgidJwtSignature agidJwtSignature;

    private final String headRequestSenderCode;
    private final String headRequestAddresseeCode;
    private final String headRequestOperationRequest;
    private final String headRequestSendType;

    private final String dataRequestPersonalDetailsRequest;

    private final ObjectMapper objectMapper;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ResidenceAssessmentRestClientImpl(AnprWebClient anprWebClient,
                                             AgidJwtSignature agidJwtSignature,

                                             @Value("${app.anpr.c020-residenceAssessment.base-url}") String residenceAssessmentBaseUrl,

                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.senderCode}") String headRequestSenderCode,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.addresseeCode}") String headRequestAddresseeCode,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.operationRequest}") String headRequestOperationRequest,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.sendType}") String headRequestSendType,

                                             @Value("${app.anpr.c020-residenceAssessment.properties.dataRequest.personalDetailsRequest}") String dataRequestPersonalDetailsRequest,

                                             ObjectMapper objectMapper) {
        this.agidJwtSignature = agidJwtSignature;

        this.objectMapper = objectMapper;

        HttpClient httpClient = anprWebClient.getHttpClientSecure();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(residenceAssessmentBaseUrl)
                .build();


        this.headRequestSenderCode = headRequestSenderCode;
        this.headRequestAddresseeCode = headRequestAddresseeCode;
        this.headRequestOperationRequest = headRequestOperationRequest;
        this.headRequestSendType = headRequestSendType;
        this.dataRequestPersonalDetailsRequest = dataRequestPersonalDetailsRequest;
    }

    @Override
    public Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode) {
//        return e002ServiceApi.e002(generateRequest(fiscalCode));//TODO define error code for retry
        String requestDtoString = convertToJson(generateRequest(fiscalCode));
        String digest = createDigestFromPayload(requestDtoString);
        return webClient.post()
                .uri("/anpr-service-e002")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(accessToken);
                    httpHeaders.add("Agid-JWT-Signature", agidJwtSignature.createAgidJwt(digest));
                    httpHeaders.add("Content-Encoding", "UTF-8");
                    httpHeaders.add("Digest", digest);
                })
                .bodyValue(requestDtoString)
                .retrieve()
                .bodyToMono(RispostaE002OKDTO.class);
    }

    private String convertToJson(RichiestaE002DTO requestE002DTO) {
        try {
            return objectMapper.writeValueAsString(requestE002DTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String createDigestFromPayload(String request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
            return "SHA-256="+ Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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