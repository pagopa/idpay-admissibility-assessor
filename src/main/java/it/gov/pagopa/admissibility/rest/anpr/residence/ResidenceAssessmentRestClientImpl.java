package it.gov.pagopa.admissibility.rest.anpr.residence;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.repository.CustomSequenceGeneratorGeneratorRepository;
import it.gov.pagopa.admissibility.rest.agid.AnprJwtSignature;
import it.gov.pagopa.admissibility.rest.anpr.AnprWebClient;
import it.gov.pagopa.admissibility.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class ResidenceAssessmentRestClientImpl implements ResidenceAssessmentRestClient{
    private final  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final  SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    private final WebClient webClient;
    private final AnprJwtSignature anprJwtSignature;

    private final String headRequestSenderCode;
    private final String headRequestAddresseeCode;
    private final String headRequestOperationRequest;
    private final String headRequestSendType;

    private final String dataRequestPersonalDetailsRequest;

    private final CustomSequenceGeneratorGeneratorRepository customSequenceGeneratorRepository;

    private final ObjectMapper objectMapper;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ResidenceAssessmentRestClientImpl(AnprWebClient anprWebClient,
                                             AnprJwtSignature anprJwtSignature,

                                             @Value("${app.anpr.c020-residenceAssessment.base-url}") String residenceAssessmentBaseUrl,

                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.senderCode}") String headRequestSenderCode,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.addresseeCode}") String headRequestAddresseeCode,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.operationRequest}") String headRequestOperationRequest,
                                             @Value("${app.anpr.c020-residenceAssessment.properties.headRequest.sendType}") String headRequestSendType,

                                             @Value("${app.anpr.c020-residenceAssessment.properties.dataRequest.personalDetailsRequest}") String dataRequestPersonalDetailsRequest,

                                             CustomSequenceGeneratorGeneratorRepository customSequenceGeneratorRepository, ObjectMapper objectMapper) {
        this.anprJwtSignature = anprJwtSignature;
        this.customSequenceGeneratorRepository = customSequenceGeneratorRepository;

        this.objectMapper = objectMapper;

        this.webClient = anprWebClient.getAnprWebClient()
                .clone()
                .baseUrl(residenceAssessmentBaseUrl)
                .build();

        this.headRequestSenderCode = headRequestSenderCode;
        this.headRequestAddresseeCode = headRequestAddresseeCode;
        this.headRequestOperationRequest = headRequestOperationRequest;
        this.headRequestSendType = headRequestSendType;
        this.dataRequestPersonalDetailsRequest = dataRequestPersonalDetailsRequest;
    }

    @Override
    public Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode, AgidJwtTokenPayload agidJwtTokenPayload) {
        return generateRequest(fiscalCode)
                .flatMap(richiestaE002DTO -> callAnprService(accessToken, agidJwtTokenPayload, richiestaE002DTO));
    }

    private Mono<RispostaE002OKDTO> callAnprService(String accessToken, AgidJwtTokenPayload agidJwtTokenPayload, RichiestaE002DTO richiestaE002DTO) {
        String requestDtoString = Utils.convertToJson(richiestaE002DTO, objectMapper);
        String digest = Utils.createSHA256Digest(requestDtoString);
        return webClient.post()
                .uri("/anpr-service-e002")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.setBearerAuth(accessToken);
                    httpHeaders.add("Agid-JWT-Signature", anprJwtSignature.createAgidJwt(digest, agidJwtTokenPayload));
                    httpHeaders.add("Content-Encoding", "UTF-8");
                    httpHeaders.add("Digest", digest);
                })
                .bodyValue(requestDtoString)
                .retrieve()
                .bodyToMono(RispostaE002OKDTO.class)
                .doOnError(e -> {
                    throw new AnprDailyRequestLimitException(e);
                });
        //TODO define error code for retry
    }

    private Mono<RichiestaE002DTO> generateRequest(String fiscalCode) {
        return customSequenceGeneratorRepository.nextValue(OnboardingConstants.ANPR_E002_INVOKE)
                .map(sequenceValue -> createRequest(fiscalCode, sequenceValue));
    }

    private RichiestaE002DTO createRequest(String fiscalCode, Long sequenceValue) {
        Date dateNow = new Date();
        TipoTestataRichiestaE000DTO testataRichiestaE000DTO = new TipoTestataRichiestaE000DTO()
                .idOperazioneClient(String.valueOf(sequenceValue))
                .codMittente(headRequestSenderCode)
                .codDestinatario(headRequestAddresseeCode)
                .operazioneRichiesta(headRequestOperationRequest)
                .dataOraRichiesta(datetimeFormat.format(dateNow))
                .tipoOperazione("C")
                .tipoInvio(headRequestSendType);

        TipoCriteriRicercaE002DTO criteriRicercaE002DTO = new TipoCriteriRicercaE002DTO()
                .codiceFiscale(fiscalCode);

        TipoDatiRichiestaE002DTO datiRichiestaE002DTO = new TipoDatiRichiestaE002DTO()
                .schedaAnagraficaRichiesta(dataRequestPersonalDetailsRequest)
                .dataRiferimentoRichiesta(dateFormat.format(dateNow))
                .casoUso("C020");

        return new RichiestaE002DTO()
                .testataRichiesta(testataRichiestaE000DTO)
                .criteriRicerca(criteriRicercaE002DTO)
                .datiRichiesta(datiRichiestaE002DTO);
    }

}