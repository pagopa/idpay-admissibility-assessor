package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;
import it.gov.pagopa.admissibility.soap.inps.IseeConsultationSoapClient;
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
import java.util.Optional;

@Service
public class InpsInvocationServiceServiceImpl implements InpsInvocationService {

    private final IseeConsultationSoapClient iseeConsultationSoapClient;

    public InpsInvocationServiceServiceImpl(IseeConsultationSoapClient iseeConsultationSoapClient) {
        this.iseeConsultationSoapClient = iseeConsultationSoapClient;
    }

    @Override
    public Mono<Optional<ConsultazioneIndicatoreResponseType>> invoke(String fiscalCode) {
        return iseeConsultationSoapClient.getIsee(fiscalCode).map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    @Override
    public void extract(ConsultazioneIndicatoreResponseType inpsResponse, boolean getIsee, OnboardingDTO onboardingRequest) {
        if (inpsResponse != null && getIsee) {
            // TODO log userId and ISEE obtained from INPS
            onboardingRequest.setIsee(getIseeFromResponse(inpsResponse));
        }
    }

    private BigDecimal getIseeFromResponse(ConsultazioneIndicatoreResponseType inpsResponse) {
        String inpsResultString = new String(inpsResponse.getXmlEsitoIndicatore());

        TypeEsitoConsultazioneIndicatore inpsResult = readResultFromXmlString(inpsResultString);
        return inpsResult.getISEE();
    }

    private TypeEsitoConsultazioneIndicatore readResultFromXmlString(String inpsResultString) {
        try (StringReader sr = new StringReader(inpsResultString)) {

            XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xmlFactory.createXMLStreamReader(sr);
            JAXBContext jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = unmarshaller.unmarshal(xsr, TypeEsitoConsultazioneIndicatore.class);
            return je.getValue();
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("[ADMISSIBILITY][INPS_INVOCATION] Cannot read XmlEsitoIndicatore to get ISEE from INPS' response", e);
        }
    }
}
