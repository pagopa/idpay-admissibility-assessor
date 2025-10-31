package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsThresholdRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.*;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.xml.datatype.DatatypeFactory;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.common.reactive.web.ReactiveRequestContextHolder.getRequest;

@RestController
@Slf4j
public class TestConnectionControllerImpl implements TestConnectionController{

    private static final DatatypeFactory DATATYPE_FACTORY = DatatypeFactory.newDefaultInstance();

    private final InpsThresholdRetrieverService inpsThresholdRetrieverService;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    private final ISvcConsultazione portSvcConsultazione;

    public TestConnectionControllerImpl(InpsThresholdRetrieverService inpsThresholdRetrieverService, PagoPaAnprPdndConfig pagoPaAnprPdndConfig, InpsThresholdClientConfig inpsThresholdClientConfig) {
        this.inpsThresholdRetrieverService = inpsThresholdRetrieverService;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
        this.portSvcConsultazione = inpsThresholdClientConfig.getPortSvcConsultazione();
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> getThreshold(String threshold, String userCode) {
        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(false, Collections.emptyList(), false, false, true, threshold);
        return inpsThresholdRetrieverService.invoke(userCode,pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c001"), pdndServicesInvocation,  new OnboardingDTO());

    }

    @Override
    public Mono<ConsultazioneSogliaIndicatoreResponse> getThresholdV2(String threshold, String userCode) {

        ConsultazioneSogliaIndicatoreRequestType consultazioneSogliaIndicatoreRequestType = new ConsultazioneSogliaIndicatoreRequestType();

        consultazioneSogliaIndicatoreRequestType.setCodiceFiscale(userCode);
        consultazioneSogliaIndicatoreRequestType.setCodiceSoglia(threshold);
        consultazioneSogliaIndicatoreRequestType.setFornituraNucleo(SiNoEnum.NO);
        consultazioneSogliaIndicatoreRequestType.setDataValidita(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));

        return SoapUtils.soapInvoke2Mono(asyncHandler ->
                portSvcConsultazione.consultazioneSogliaIndicatoreAsync(consultazioneSogliaIndicatoreRequestType, asyncHandler));

    }
}
