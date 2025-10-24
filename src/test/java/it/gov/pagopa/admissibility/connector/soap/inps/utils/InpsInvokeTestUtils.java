package it.gov.pagopa.admissibility.connector.soap.inps.utils;

import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.ObjectFactory;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.TypeEsitoConsultazioneIndicatore;

import jakarta.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;

public class InpsInvokeTestUtils {

    //region build responses
    public static ConsultazioneIndicatoreResponseType buildInpsResponse(EsitoEnum outcome) {
        ConsultazioneIndicatoreResponseType response = new ConsultazioneIndicatoreResponseType();
        response.setEsito(outcome);

        response.setXmlEsitoIndicatore(buildXmlResult());

        return response;
    }

    public static byte[] buildXmlResult() {
        return buildXmlResult(BigDecimal.valueOf(10000));
    }

    public static byte[] buildXmlResult(BigDecimal isee) {
        TypeEsitoConsultazioneIndicatore xmlResult = new TypeEsitoConsultazioneIndicatore();
        xmlResult.setISEE(isee);

        return toByteArray(xmlResult);
    }

    public static byte[] toByteArray(TypeEsitoConsultazioneIndicatore inpsResult) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = new ObjectFactory().createIndicatore(inpsResult);
            StringWriter sw = new StringWriter();

            marshaller.marshal(je, sw);

            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JAXBException e){
            throw new IllegalStateException("Cannot create mocked INPS response", e);
        }
    }

    public static byte[] encodeByteArrayInB64(byte[] input) {
        byte[] result = Base64.getEncoder().encode(input);
        System.out.println("ENCODED BYTE ARRAY --------- " + new String(result));
        return result;
    }


    //endregion
    
    //region get data from response's models
    public static BigDecimal getIseeFromResponse(ConsultazioneIndicatoreResponseType inpsResponse) {
        String inpsResultString = new String(inpsResponse.getXmlEsitoIndicatore());

        TypeEsitoConsultazioneIndicatore inpsResult = readResultFromXmlString(inpsResultString);
        return inpsResult.getISEE();
    }

    public static TypeEsitoConsultazioneIndicatore readResultFromXmlString(String inpsResultString) {
        try (StringReader sr = new StringReader(inpsResultString)) {

            XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xmlFactory.createXMLStreamReader(sr);
            JAXBContext jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            JAXBElement<TypeEsitoConsultazioneIndicatore> je = unmarshaller.unmarshal(xsr, TypeEsitoConsultazioneIndicatore.class);
            return je.getValue();
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("[ONBOARDING_REQUEST][INPS_INVOCATION] Cannot read XmlEsitoIndicatore to get ISEE from INPS' response", e);
        }
    }

    public static TipoResidenzaDTO getResidenceFromAnswer(RispostaE002OKDTO anprAnswer) {
        return anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
    }

    public static BirthDate getBirthDateFromAnswer(RispostaE002OKDTO anprAnswer) {
        String year = anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getSenzaGiornoMese();
        Integer age = Period.between(
                        LocalDate.parse(anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getDataNascita()),
                        LocalDate.now())
                .getYears();

        return new BirthDate(year, age);
    }
    //endregion
}
