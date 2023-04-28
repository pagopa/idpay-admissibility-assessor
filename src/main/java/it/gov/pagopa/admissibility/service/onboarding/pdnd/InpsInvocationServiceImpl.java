package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.connector.soap.inps.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InpsInvocationServiceImpl implements InpsInvocationService {

    private final CriteriaCodeService criteriaCodeService;
    private final IseeConsultationSoapClient iseeConsultationSoapClient;
    private final XMLInputFactory xmlFactory;
    private final JAXBContext jaxbContext;

    public InpsInvocationServiceImpl(CriteriaCodeService criteriaCodeService, IseeConsultationSoapClient iseeConsultationSoapClient) throws JAXBException {
        this.criteriaCodeService = criteriaCodeService;
        this.iseeConsultationSoapClient = iseeConsultationSoapClient;
        this.xmlFactory = XMLInputFactory.newFactory();
        this.jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
    }

    @Override
    public Mono<Optional<ConsultazioneIndicatoreResponseType>> invoke(String fiscalCode, IseeTypologyEnum iseeType) {
        return iseeConsultationSoapClient.getIsee(fiscalCode, iseeType).map(Optional::of)
                .defaultIfEmpty(Optional.empty());
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
        String inpsResultString = new String(inpsResponse.getXmlEsitoIndicatore());

        TypeEsitoConsultazioneIndicatore inpsResult = readResultFromXmlString(inpsResultString);
        return inpsResult.getISEE();
    }

    private TypeEsitoConsultazioneIndicatore readResultFromXmlString(String inpsResultString) {
        try (StringReader sr = new StringReader(inpsResultString)) {
            XMLStreamReader xsr = xmlFactory.createXMLStreamReader(sr);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = unmarshaller.unmarshal(xsr, TypeEsitoConsultazioneIndicatore.class);
            return je.getValue();
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("[ADMISSIBILITY][INPS_INVOCATION] Cannot read XmlEsitoIndicatore to get ISEE from INPS' response", e);
        }
    }
}
