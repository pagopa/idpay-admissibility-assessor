package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.DatiIndicatoreType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.SiNoEnum;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsThresholdRetrieverServiceImplTest {

    public static final String FISCAL_CODE = "fiscalCode";
    public static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG = new PdndInitiativeConfig(
            "CLIENTID",
            "KID",
            "PURPOSEID"
    );
    public static final IseeTypologyEnum ISEE_TYPOLOGY1 = IseeTypologyEnum.UNIVERSITARIO;
    public static final IseeTypologyEnum ISEE_TYPOLOGY2 = IseeTypologyEnum.CORRENTE;

    @Mock
    private IseeThresholdConsultationSoapClient iseeThresholdConsultationSoapClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsThresholdRetrieverService inpsThresholdRetrieverService;

    private ConsultazioneSogliaIndicatoreResponseType inpsResponse;
    private Boolean threshold;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        inpsThresholdRetrieverService = new InpsThresholdRetrieverServiceImpl(criteriaCodeServiceMock, iseeThresholdConsultationSoapClientMock);

        inpsResponse = new ConsultazioneSogliaIndicatoreResponseType();
        inpsResponse.setEsito(EsitoEnum.OK);
        inpsResponse.setIdRichiesta(1);
        DatiIndicatoreType datiIndicatoreType = new DatiIndicatoreType();
        datiIndicatoreType.setSottoSoglia(SiNoEnum.SI);
        inpsResponse.setDatiIndicatore(datiIndicatoreType);
        threshold = Boolean.TRUE;
    }

    private PdndServicesInvocation buildPdndServicesInvocation(boolean verifyIsee, String thresholdCode) {
        return new PdndServicesInvocation(false, List.of(ISEE_TYPOLOGY1, ISEE_TYPOLOGY2), false, false,verifyIsee, thresholdCode);
    }

    @Test
    void testInvokeOk_requireData() {
        testInvokeOk(true);
    }

    @Test
    void testInvokeOk_NorequireData() {
        testInvokeOk(false);
    }

    private void testInvokeOk(boolean verifyIsee) {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();
        if (verifyIsee) {
            Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE")).thenReturn(Mono.just(inpsResponse));
        }

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(verifyIsee, "THRESHOLD_CODE"), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());

        if (verifyIsee) {
            Assertions.assertEquals(threshold, onboardingRequest.getUnderThreshold());
        } else {
            Assertions.assertNull(onboardingRequest.getUserId());
        }
    }

    @Test
    void testInvokeOk_KoEsitoResult() {
        inpsResponse.setEsito(EsitoEnum.RICHIESTA_INVALIDA);
        inpsResponse.setDatiIndicatore(null);
        testExtractWhenNoIsee();

        DatiIndicatoreType datiIndicatoreType = new DatiIndicatoreType();
        datiIndicatoreType.setSottoSoglia(null);
        inpsResponse.setDatiIndicatore(datiIndicatoreType);
        testExtractWhenNoIsee();
    }

    @Test
    void testVerifyThresholdIseeFromResponse_underThresholdDeformed(){
        // Given
        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.SI);
        dati.setPresenzaDifformita(SiNoEnum.SI);
        inpsResponse.setDatiIndicatore(dati);

        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE"))
                .thenReturn(Mono.just(inpsResponse));

        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService
                .invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, "THRESHOLD_CODE"), onboardingRequest)
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        Assertions.assertFalse(onboardingRequest.getUnderThreshold());

    }
    @Test
    void testVerifyThresholdIseeFromResponse_underThresholdNotDeformed(){
        // Given
        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.SI);
        dati.setPresenzaDifformita(SiNoEnum.NO);
        inpsResponse.setDatiIndicatore(dati);

        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE"))
                .thenReturn(Mono.just(inpsResponse));

        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService
                .invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, "THRESHOLD_CODE"), onboardingRequest)
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        Assertions.assertTrue(onboardingRequest.getUnderThreshold());

    }

    @Test
    void testVerifyThresholdIseeFromResponse_overThresholdDeformed(){
        // Given
        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.NO);
        dati.setPresenzaDifformita(SiNoEnum.SI);
        inpsResponse.setDatiIndicatore(dati);

        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE"))
                .thenReturn(Mono.just(inpsResponse));

        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService
                .invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, "THRESHOLD_CODE"), onboardingRequest)
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        Assertions.assertFalse(onboardingRequest.getUnderThreshold());

    }
    @Test
    void testVerifyThresholdIseeFromResponse_overThresholdNotDeformed(){
        // Given
        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.NO);
        dati.setPresenzaDifformita(SiNoEnum.NO);
        inpsResponse.setDatiIndicatore(dati);

        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE"))
                .thenReturn(Mono.just(inpsResponse));

        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService
                .invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, "THRESHOLD_CODE"), onboardingRequest)
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        Assertions.assertFalse(onboardingRequest.getUnderThreshold());

    }

    private void testExtractWhenNoIsee() {
        // Given
        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE")).thenReturn(Mono.just(inpsResponse));
        Mockito.when(iseeThresholdConsultationSoapClientMock.verifyThresholdIsee(FISCAL_CODE, "THRESHOLD_CODE")).thenReturn(Mono.just(inpsResponse));
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsThresholdRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true, "THRESHOLD_CODE"), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(List.of(buildExpectedIseeKoRejectionReason()), result.get());
        Assertions.assertNull(onboardingRequest.getIsee());
    }

    private OnboardingRejectionReason buildExpectedIseeKoRejectionReason() {
        return new OnboardingRejectionReason(
                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH_LABEL,
                "Soglia ISEE non disponibile"
        );
    }
}