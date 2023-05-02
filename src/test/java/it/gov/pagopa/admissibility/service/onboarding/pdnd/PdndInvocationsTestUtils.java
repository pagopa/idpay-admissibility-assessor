package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ObjectFactory;
import it.gov.pagopa.admissibility.generated.soap.ws.client.TypeEsitoConsultazioneIndicatore;

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
import java.util.List;

public class PdndInvocationsTestUtils {

    //region build responses
    public static ConsultazioneIndicatoreResponseType buildInpsResponse(EsitoEnum outcome) throws JAXBException {
        ConsultazioneIndicatoreResponseType response = new ConsultazioneIndicatoreResponseType();
        response.setEsito(outcome);

        response.setXmlEsitoIndicatore(buildXmlResult());

        return response;
    }

    public static byte[] buildXmlResult() throws JAXBException {
        TypeEsitoConsultazioneIndicatore xmlResult = new TypeEsitoConsultazioneIndicatore();
        xmlResult.setISEE(BigDecimal.valueOf(10000));

        return toByteArray(xmlResult);
    }

    public static byte[] toByteArray(TypeEsitoConsultazioneIndicatore inpsResult) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(TypeEsitoConsultazioneIndicatore.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        JAXBElement<TypeEsitoConsultazioneIndicatore> je = new ObjectFactory().createIndicatore(inpsResult);
        StringWriter sw = new StringWriter();

        marshaller.marshal(je, sw);

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeByteArrayInB64(byte[] input) {
        byte[] result = Base64.getEncoder().encode(input);
        System.out.println("ENCODED BYTE ARRAY --------- " + new String(result));
        return result;
    }

    public static RispostaE002OKDTO buildAnprResponse() {

        return new RispostaE002OKDTO().listaSoggetti(buildListaSoggetti());
    }

    public static TipoListaSoggettiDTO buildListaSoggetti() {
        TipoGeneralitaDTO generalita = new TipoGeneralitaDTO();
        generalita.setDataNascita("2001-02-04");
        generalita.setSenzaGiornoMese("2001");

        TipoComuneDTO comune = new TipoComuneDTO();
        comune.setNomeComune("Milano");
        comune.setSiglaProvinciaIstat("MI");

        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
        indirizzo.setCap("20143");
        indirizzo.setComune(comune);

        TipoResidenzaDTO residenza = new TipoResidenzaDTO();
        residenza.setIndirizzo(indirizzo);

        TipoDatiSoggettiEnteDTO datiSoggetto = new TipoDatiSoggettiEnteDTO();
        datiSoggetto.setGeneralita(generalita);
        datiSoggetto.setResidenza(List.of(residenza));

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        listaSoggetti.setDatiSoggetto(List.of(datiSoggetto));

        return listaSoggetti;
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
