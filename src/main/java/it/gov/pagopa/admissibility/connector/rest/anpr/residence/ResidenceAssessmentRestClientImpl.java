package it.gov.pagopa.admissibility.connector.rest.anpr.residence;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.rest.agid.AnprJwtSignature;
import it.gov.pagopa.admissibility.connector.rest.anpr.AnprWebClient;
import it.gov.pagopa.admissibility.connector.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class ResidenceAssessmentRestClientImpl implements ResidenceAssessmentRestClient{
    private final  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final WebClient webClient;
    private final AnprJwtSignature anprJwtSignature;

    private final CustomSequenceGeneratorRepository customSequenceGeneratorRepository;

    private final ObjectMapper objectMapper;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ResidenceAssessmentRestClientImpl(AnprWebClient anprWebClient,
                                             AnprJwtSignature anprJwtSignature,

                                             @Value("${app.anpr.c001-residenceAssessment.base-url}") String residenceAssessmentBaseUrl,

                                             CustomSequenceGeneratorRepository customSequenceGeneratorRepository, ObjectMapper objectMapper) {
        this.anprJwtSignature = anprJwtSignature;
        this.customSequenceGeneratorRepository = customSequenceGeneratorRepository;

        this.objectMapper = objectMapper;

        this.webClient = anprWebClient.getAnprWebClient()
                .clone()
                .baseUrl(residenceAssessmentBaseUrl)
                .build();
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
                    httpHeaders.setBearerAuth(accessToken);
                    httpHeaders.add("Digest", digest);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.add("Content-Encoding", "UTF-8");
                    httpHeaders.add("Agid-JWT-TrackingEvidence", anprJwtSignature.getTrackingEvidence());
                    httpHeaders.add("Agid-JWT-Signature", anprJwtSignature.createAgidJwt(digest, agidJwtTokenPayload));
                })
                .bodyValue(requestDtoString)
                .retrieve()
                .bodyToMono(RispostaE002OKDTO.class)

                //TODO define error code for retry
                .doOnError(WebClientResponseException.TooManyRequests.class, e -> {
                    throw new AnprDailyRequestLimitException(e);
                })
                .onErrorResume(e -> {
                    log.error("Something went wrong when invoking ANPR: {}", e.getMessage(), e);
                    return Mono.just(new RispostaE002OKDTO());
                });
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