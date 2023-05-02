package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.soap.inps.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
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
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IseeDataRetrieverServiceImpl implements IseeDataRetrieverService {

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

    public IseeDataRetrieverServiceImpl(CriteriaCodeService criteriaCodeService, IseeConsultationSoapClient iseeConsultationSoapClient) {
        this.criteriaCodeService = criteriaCodeService;
        this.iseeConsultationSoapClient = iseeConsultationSoapClient;
    }

    @Override
    public Mono<Optional<ConsultazioneIndicatoreResponseType>> invoke(String fiscalCode, IseeTypologyEnum iseeType) {
        // TODO why pdnd's accessToken is not used?
        return iseeConsultationSoapClient.getIsee(fiscalCode, iseeType).map(Optional::of)
                .defaultIfEmpty(Optional.empty())

                .onErrorResume(InpsDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][INPS_INVOCATION] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date
                    return Mono.just(Optional.empty());
                });
    }

    @Override
    public void extract(ConsultazioneIndicatoreResponseType inpsResponse, boolean getIsee, OnboardingDTO onboardingRequest) {
        if (inpsResponse != null && getIsee) {
            // TODO log userId and ISEE obtained from INPS
            onboardingRequest.setIsee(getIseeFromResponse(inpsResponse));

            CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_ISEE);
            if (onboardingRequest.getIsee() == null) {
                throw new OnboardingException(
                        List.of(new OnboardingRejectionReason(
                                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                                criteriaCodeConfig.getAuthority(),
                                criteriaCodeConfig.getAuthorityLabel(),
                                "ISEE non disponibile"
                        )),
                        "User having id %s has not compatible ISEE type for initiative %s".formatted(onboardingRequest.getUserId(), onboardingRequest.getInitiativeId())
                );
            }
        }
    }

    private BigDecimal getIseeFromResponse(ConsultazioneIndicatoreResponseType inpsResponse) {
        String inpsResultString = new String(inpsResponse.getXmlEsitoIndicatore(), StandardCharsets.UTF_8);

        TypeEsitoConsultazioneIndicatore inpsResult = readResultFromXmlString(inpsResultString);
        return inpsResult.getISEE();
    }

    public static TypeEsitoConsultazioneIndicatore readResultFromXmlString(String inpsResultString) {
        try (StringReader sr = new StringReader(inpsResultString)) {
            XMLStreamReader xsr = xmlFactory.createXMLStreamReader(sr);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = unmarshaller.unmarshal(xsr, TypeEsitoConsultazioneIndicatore.class);
            return je.getValue();
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("[ONBOARDING_REQUEST][INPS_INVOCATION] Cannot read XmlEsitoIndicatore to get ISEE from INPS' response", e);
        }
    }
}
