package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.config.AnprConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdnd.components.JwtSignAlgorithmRetrieverService;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.service.BaseRestPdndServiceClient;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public abstract class BaseAnprRestClientImpl<T, R, E> extends BaseRestPdndServiceClient<T, R, E> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final CustomSequenceGeneratorRepository customSequenceGeneratorRepository;

    protected BaseAnprRestClientImpl(
            ObjectMapper objectMapper,
            PdndConfig pdndConfig,
            AnprConfig anprConfig,
            BasePdndServiceConfig serviceConfig,
            JwtSignAlgorithmRetrieverService jwtSignAlgorithmRetrieverService,
            PdndRestClient pdndRestClient,
            WebClient.Builder webClientBuilder,
            HttpClient httpClient,
            CustomSequenceGeneratorRepository customSequenceGeneratorRepository,
            Class<R> responseType,
            Class<E> responseErrorType) {
        super(buildDefaultPdndServiceConfig(anprConfig, serviceConfig, responseType, responseErrorType), objectMapper, pdndConfig, jwtSignAlgorithmRetrieverService, pdndRestClient, webClientBuilder, httpClient);
        this.customSequenceGeneratorRepository = customSequenceGeneratorRepository;
    }

    //TODO re-check
    public Mono<?> invoke(String fiscalCode, PdndInitiativeConfig pdndInitiativeConfig) {
        return generateRequest(fiscalCode)
                .flatMap(request -> invokePdndRestService(
                        h -> {},
                        request,
                        pdndInitiativeConfig
                ));
    }

    private Mono<T> generateRequest(String fiscalCode) {
        return customSequenceGeneratorRepository.nextValue(OnboardingConstants.ANPR_E002_INVOKE)
                .map(sequenceValue -> createRequest(fiscalCode, sequenceValue));
    }

    protected abstract T createRequest(String fiscalCode, Long sequenceValue);

    protected String getCurrentDate() {
        return dateFormat.format(new Date());
    }
}
