package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.utils.InpsInvokeTestUtils;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.indicatore.EsitoEnum;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsDataRetrieverServiceImplTest {

    private static final String FISCAL_CODE = "FISCAL_CODE";

    private static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG =
            new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID");

    @Mock
    private IseeConsultationSoapClient iseeConsultationSoapClientMock;

    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsDataRetrieverService service;

    private ConsultazioneIndicatoreResponseType inpsOkResponse;
    private BigDecimal expectedIsee;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        service = new InpsDataRetrieverServiceImpl(
                criteriaCodeServiceMock,
                iseeConsultationSoapClientMock,
                IseeTypologyEnum.ORDINARIO
        );

        inpsOkResponse = InpsInvokeTestUtils.buildInpsResponse(EsitoEnum.OK);
        expectedIsee = BigDecimal.valueOf(10000);
    }


    private PdndServicesInvocation buildIseeInvocation(boolean verify) {
        return new PdndServicesInvocation(
                OnboardingConstants.CRITERIA_CODE_ISEE.toLowerCase(),
                verify,
                null
        );
    }


    @Test
    void testInvokeOk_requireIsee() {
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Mockito.when(iseeConsultationSoapClientMock.getIsee(Mockito.eq(FISCAL_CODE), Mockito.any()))
                .thenReturn(Mono.just(inpsOkResponse));

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildIseeInvocation(true),
                        onboardingRequest
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        TestUtils.assertBigDecimalEquals(expectedIsee, onboardingRequest.getIsee());
    }

    @Test
    void testInvokeOk_noRequireIsee() {
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildIseeInvocation(false),
                        onboardingRequest
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());
        Assertions.assertNull(onboardingRequest.getIsee());

        Mockito.verifyNoInteractions(iseeConsultationSoapClientMock);
    }

    @Test
    void testInvokeRetryableError() {
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Mockito.when(iseeConsultationSoapClientMock.getIsee(Mockito.eq(FISCAL_CODE), Mockito.any()))
                .thenReturn(Mono.empty());

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildIseeInvocation(true),
                        onboardingRequest
                ).block();

        Assertions.assertNull(result);
        Assertions.assertNull(onboardingRequest.getIsee());
    }

    @Test
    void testInvokeKoIsee() {
        inpsOkResponse.setEsito(EsitoEnum.RICHIESTA_INVALIDA);
        inpsOkResponse.setXmlEsitoIndicatore("".getBytes(StandardCharsets.UTF_8));

        Mockito.when(iseeConsultationSoapClientMock.getIsee(Mockito.eq(FISCAL_CODE), Mockito.any()))
                .thenReturn(Mono.just(inpsOkResponse));

        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Optional<List<OnboardingRejectionReason>> result =
                service.invoke(
                        FISCAL_CODE,
                        PDND_INITIATIVE_CONFIG,
                        buildIseeInvocation(true),
                        onboardingRequest
                ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(
                List.of(buildExpectedIseeKoRejectionReason()),
                result.get()
        );
        Assertions.assertNull(onboardingRequest.getIsee());
    }

    private OnboardingRejectionReason buildExpectedIseeKoRejectionReason() {
        return new OnboardingRejectionReason(
                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH_LABEL,
                "ISEE non disponibile"
        );
    }
}