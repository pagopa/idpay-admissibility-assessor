package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.VerifyDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.DatiIndicatoreType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.SiNoEnum;
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

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsThresholdRetrieverServiceImplTest {

    private static final String FISCAL_CODE = "FISCAL_CODE";
    private static final String THRESHOLD_CODE = "THRESHOLD_CODE";

    private static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG =
            new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID");

    @Mock
    private IseeThresholdConsultationSoapClient iseeThresholdConsultationSoapClientMock;

    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsThresholdRetrieverService service;

    private ConsultazioneSogliaIndicatoreResponseType inpsOkResponse;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        service = new InpsThresholdRetrieverServiceImpl(
                criteriaCodeServiceMock,
                iseeThresholdConsultationSoapClientMock
        );

        inpsOkResponse = new ConsultazioneSogliaIndicatoreResponseType();
        inpsOkResponse.setEsito(EsitoEnum.OK);

        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.SI);
        dati.setPresenzaDifformita(SiNoEnum.NO);
        inpsOkResponse.setDatiIndicatore(dati);
    }


    private VerifyDTO buildThresholdVerify() {
        return new VerifyDTO(
                OnboardingConstants.CRITERIA_CODE_ISEE,
                true,
                true,
                THRESHOLD_CODE,
                null,
                null,
                null
        );
    }

    private PdndServicesInvocation buildInvocation(boolean verify) {
        return new PdndServicesInvocation(
                OnboardingConstants.CRITERIA_CODE_ISEE,
                verify,
                THRESHOLD_CODE
        );
    }

    private OnboardingDTO onboardingWithVerify(VerifyDTO verify) {
        OnboardingDTO onboarding = new OnboardingDTO();
        onboarding.setVerifies(List.of(verify));
        return onboarding;
    }

    @Test
    void testInvoke_skipVerification() {
        VerifyDTO verify = buildThresholdVerify();
        OnboardingDTO onboarding = onboardingWithVerify(verify);

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildInvocation(false),
                        onboarding
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().isEmpty());
        Assertions.assertNull(verify.getResult());

        Mockito.verifyNoInteractions(iseeThresholdConsultationSoapClientMock);
    }

    @Test
    void testInvoke_ok_underThresholdNotDeformed() {
        VerifyDTO verify = buildThresholdVerify();
        OnboardingDTO onboarding = onboardingWithVerify(verify);

        Mockito.when(
                iseeThresholdConsultationSoapClientMock
                        .verifyThresholdIsee(FISCAL_CODE, THRESHOLD_CODE)
        ).thenReturn(Mono.just(inpsOkResponse));

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildInvocation(true),
                        onboarding
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().isEmpty());
        Assertions.assertNull(verify.getResult());
    }

    @Test
    void testInvoke_ko_underThresholdDeformed() {
        DatiIndicatoreType dati = new DatiIndicatoreType();
        dati.setSottoSoglia(SiNoEnum.SI);
        dati.setPresenzaDifformita(SiNoEnum.SI);
        inpsOkResponse.setDatiIndicatore(dati);

        VerifyDTO verify = buildThresholdVerify();
        OnboardingDTO onboarding = onboardingWithVerify(verify);

        Mockito.when(
                iseeThresholdConsultationSoapClientMock
                        .verifyThresholdIsee(FISCAL_CODE, THRESHOLD_CODE)
        ).thenReturn(Mono.just(inpsOkResponse));

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildInvocation(true),
                        onboarding
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertFalse(result.get().isEmpty());
        Assertions.assertNull(verify.getResult());
    }

    @Test
    void testInvoke_ko_missingData() {
        inpsOkResponse.setDatiIndicatore(null);

        VerifyDTO verify = buildThresholdVerify();
        OnboardingDTO onboarding = onboardingWithVerify(verify);

        Mockito.when(
                iseeThresholdConsultationSoapClientMock
                        .verifyThresholdIsee(FISCAL_CODE, THRESHOLD_CODE)
        ).thenReturn(Mono.just(inpsOkResponse));

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildInvocation(true),
                        onboarding
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertFalse(result.get().isEmpty());
        Assertions.assertNull(verify.getResult());
    }

    @Test
    void testInvoke_emptyResponse_retry() {
        VerifyDTO verify = buildThresholdVerify();
        OnboardingDTO onboarding = onboardingWithVerify(verify);

        Mockito.when(
                iseeThresholdConsultationSoapClientMock
                        .verifyThresholdIsee(FISCAL_CODE, THRESHOLD_CODE)
        ).thenReturn(Mono.empty());

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildInvocation(true),
                        onboarding
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
        Assertions.assertNull(verify.getResult());
    }
}