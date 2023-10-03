package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
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

    public static final Mono<Optional<List<OnboardingRejectionReason>>> MONO_EMPTY_RESPONSE = Mono.just(Optional.empty());

    private static final XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
    private static final JAXBContext jaxbContext;

    private final CriteriaCodeService criteriaCodeService;
    private final IseeConsultationSoapClient iseeConsultationSoapClient;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Something gone wrong while configuring JAXB serializer", e);
        }
    }

    public InpsDataRetrieverServiceImpl(CriteriaCodeService criteriaCodeService, IseeConsultationSoapClient iseeConsultationSoapClient) {
        this.criteriaCodeService = criteriaCodeService;
        this.iseeConsultationSoapClient = iseeConsultationSoapClient;
    }

    private boolean accept(PdndServicesInvocation pdndServicesInvocation) {
        return pdndServicesInvocation.isGetIsee();
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation pdndServicesInvocation,
            OnboardingDTO onboardingRequest) {
        if (!accept(pdndServicesInvocation)) {
            return MONO_OPTIONAL_EMPTY_LIST;
        }
        // TODO invoke all until obtained a result
        return iseeConsultationSoapClient.getIsee(fiscalCode, pdndServicesInvocation.getIseeTypes().get(0))
                .map(inpsResponse -> Optional.of(extractData(inpsResponse, onboardingRequest)))
                .switchIfEmpty(MONO_EMPTY_RESPONSE)

                .onErrorResume(InpsDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][INPS_INVOCATION] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date
                    return MONO_EMPTY_RESPONSE;
                });
    }

    private List<OnboardingRejectionReason> extractData(ConsultazioneIndicatoreResponseType inpsResponse, OnboardingDTO onboardingRequest) {
        if (inpsResponse != null) {
            onboardingRequest.setIsee(getIseeFromResponse(inpsResponse));
            // TODO log userId and ISEE obtained from INPS
        }

        if (onboardingRequest.getIsee() == null) {
            CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_ISEE);
            log.debug("[ONBOARDING_REQUEST][INPS_INVOCATION] User having id {} has not compatible ISEE type for initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
            return List.of(new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                            OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                            criteriaCodeConfig.getAuthority(),
                            criteriaCodeConfig.getAuthorityLabel(),
                            "ISEE non disponibile"
                    )
            );
        }

        return Collections.emptyList();
    }

    private BigDecimal getIseeFromResponse(ConsultazioneIndicatoreResponseType inpsResponse) {
        if(inpsResponse.getXmlEsitoIndicatore() != null && inpsResponse.getXmlEsitoIndicatore().length>0) {
            try {
                String inpsResultString = new String(inpsResponse.getXmlEsitoIndicatore(), StandardCharsets.UTF_8);

                TypeEsitoConsultazioneIndicatore inpsResult = readResultFromXmlString(inpsResultString);
                return inpsResult.getISEE();
            } catch (Exception e) {
                log.error("Cannot read ISEE from INPS response", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public static TypeEsitoConsultazioneIndicatore readResultFromXmlString(String inpsResultString) {
        try (StringReader sr = new StringReader(inpsResultString)) {
            XMLStreamReader xsr = xmlFactory.createXMLStreamReader(sr);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = unmarshaller.unmarshal(xsr, TypeEsitoConsultazioneIndicatore.class);
            return je.getValue();
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("[ONBOARDING_REQUEST][INPS_INVOCATION] Cannot read XmlEsitoIndicatore to get ISEE from INPS response", e);
        }
    }
}
