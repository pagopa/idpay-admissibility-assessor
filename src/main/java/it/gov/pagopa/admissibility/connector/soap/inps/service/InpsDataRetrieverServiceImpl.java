package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InpsDataRetrieverServiceImpl implements InpsDataRetrieverService {

    private static final XMLInputFactory XML_FACTORY = XMLInputFactory.newFactory();
    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Error configuring JAXB serializer for INPS response", e);
        }
    }

    private final CriteriaCodeService criteriaCodeService;
    private final IseeConsultationSoapClient iseeConsultationSoapClient;

    /**
     * Tipologia ISEE TECNICA richiesta da INPS.
     */
    private final IseeTypologyEnum defaultIseeType;

    public InpsDataRetrieverServiceImpl(
            CriteriaCodeService criteriaCodeService,
            IseeConsultationSoapClient iseeConsultationSoapClient,
            @Value("${inps.isee.default-typology:ORDINARIO}") IseeTypologyEnum defaultIseeType) {

        this.criteriaCodeService = criteriaCodeService;
        this.iseeConsultationSoapClient = iseeConsultationSoapClient;
        this.defaultIseeType = defaultIseeType;
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation invocation,
            OnboardingDTO onboardingRequest) {

        // INPS base gestisce SOLO ISEE senza soglia
        if (!invocation.requirePdndInvocation()
                || !OnboardingConstants.CRITERIA_CODE_ISEE.equalsIgnoreCase(invocation.getCode())
                || invocation.getThresholdCode() != null) {

            return MONO_OPTIONAL_EMPTY_LIST;
        }

        return iseeConsultationSoapClient
                .getIsee(fiscalCode, defaultIseeType)
                .map(response ->
                        Optional.of(
                                extractData(response, onboardingRequest)
                        )
                )
                .onErrorResume(InpsDailyRequestLimitException.class, e -> {
                    log.debug(
                            "[ONBOARDING_REQUEST][INPS] Daily limit occurred when calling INPS service",
                            e
                    );
                    return MONO_EMPTY_RESPONSE;
                });
    }

    /**
     * Il retriever NON decide KO finale.
     * Restituisce:
     * - emptyList => verifica OK
     * - list con KO => verifica KO
     */
    private List<OnboardingRejectionReason> extractData(
            ConsultazioneIndicatoreResponseType inpsResponse,
            OnboardingDTO onboardingRequest) {

        if (inpsResponse != null) {
            onboardingRequest.setIsee(getIseeFromResponse(inpsResponse));
        }

        if (onboardingRequest.getIsee() == null) {
            CriteriaCodeConfig cfg =
                    criteriaCodeService.getCriteriaCodeConfig(
                            OnboardingConstants.CRITERIA_CODE_ISEE.toLowerCase()
                    );

            return List.of(
                    new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                            OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                            cfg.getAuthority(),
                            cfg.getAuthorityLabel(),
                            "ISEE non disponibile"
                    )
            );
        }

        return Collections.emptyList();
    }

    private BigDecimal getIseeFromResponse(
            ConsultazioneIndicatoreResponseType inpsResponse) {

        if (inpsResponse.getXmlEsitoIndicatore() == null
                || inpsResponse.getXmlEsitoIndicatore().length == 0) {
            return null;
        }

        try {
            String xml =
                    new String(
                            inpsResponse.getXmlEsitoIndicatore(),
                            StandardCharsets.UTF_8
                    );

            TypeEsitoConsultazioneIndicatore esito =
                    readResultFromXmlString(xml);

            return esito.getISEE();

        } catch (Exception e) {
            log.error(
                    "[ONBOARDING_REQUEST][INPS] Cannot parse ISEE from INPS response",
                    e
            );
            return null;
        }
    }

    static TypeEsitoConsultazioneIndicatore readResultFromXmlString(
            String xml) {

        try (StringReader sr = new StringReader(xml)) {

            XMLStreamReader xsr =
                    XML_FACTORY.createXMLStreamReader(sr);

            Unmarshaller unmarshaller =
                    JAXB_CONTEXT.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je =
                    unmarshaller.unmarshal(
                            xsr,
                            TypeEsitoConsultazioneIndicatore.class
                    );

            return je.getValue();

        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException(
                    "[ONBOARDING_REQUEST][INPS] Error reading XmlEsitoIndicatore",
                    e
            );
        }
    }
}