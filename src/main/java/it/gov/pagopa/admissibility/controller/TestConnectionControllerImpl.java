package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreRequestType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponse;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ISvcConsultazione;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.SiNoEnum;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;

@RestController
@Slf4j
public class TestConnectionControllerImpl implements TestConnectionController{

    private static final DatatypeFactory DATATYPE_FACTORY = DatatypeFactory.newDefaultInstance();


    private final ISvcConsultazione portSvcConsultazione;

    public TestConnectionControllerImpl(InpsThresholdClientConfig inpsThresholdClientConfig) {
        this.portSvcConsultazione = inpsThresholdClientConfig.getPortSvcConsultazione();
    }

    @Override
    public Mono<ConsultazioneSogliaIndicatoreResponse> getThreshold(String threshold, String userCode, String date) {

        ConsultazioneSogliaIndicatoreRequestType consultazioneSogliaIndicatoreRequestType = new ConsultazioneSogliaIndicatoreRequestType();

        consultazioneSogliaIndicatoreRequestType.setCodiceFiscale(userCode);
        consultazioneSogliaIndicatoreRequestType.setCodiceSoglia(threshold);
        consultazioneSogliaIndicatoreRequestType.setFornituraNucleo(SiNoEnum.NO);
        consultazioneSogliaIndicatoreRequestType.setDataValidita(DATATYPE_FACTORY.newXMLGregorianCalendar(date));

        return SoapUtils.soapInvoke2Mono(asyncHandler ->
                portSvcConsultazione.consultazioneSogliaIndicatoreAsync(consultazioneSogliaIndicatoreRequestType, asyncHandler));

    }
}
