package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.AnprInvocationService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.InpsInvocationService;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.RestTestUtils;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.service.AuthoritiesDataRetrieverServiceImpl=WARN",
})
class AuthoritiesDataRetrieverServiceIntegrationTest extends BaseIntegrationTest {
    public static final String ENCRYPTED_API_KEY_CLIENT_ID = "a5vd3W7VnhR5Sv44qxgXonZIlMAX9cWnCRiQq5h8";
    public static final String ENCRYPTED_API_KEY_CLIENT_ASSERTION = "a5vd3W7VnhR5Sv44ow+VbR5Rq7pM4szbB4K1dStMpnGo5dcnUR2IBR5CzLSPKzsSww4VREWnkAveCBYncyhm7Hvlh0t/LmgB/s2I57F+ArGFqUVHQbnWbKjBr28onLf5AZAPPX2M50IIQs1zZdEDgNlVtkOb37dZzoAJuA0o3fu6nvmNft6gyolK6nbvXHZtRI+ftTuTl1bHj+VfTn7JN1HG/3KG0G+oX5jphoeV4Nqy0GnCTuXh2zSDN+xdkPFn3lQ4JjFYC+9IhJ29JsLdpUG4DFpnFPXNYP4z/NpuKUoI7Dpc/WdUVegntXarLyz+1SZMY0i3m6paULNvqZzqiPDLZ2Q8zm1p7mD1LGmJtAD0NIXMlAsYxoH2Ww3O/736ab8dQjRwg+eMwakMhGltnxcn7M0Q7tU30kuT5p6YHFGQAk6OuaxiJg619kDVRbLwj+tNZE9QS/riL4ejD4hgxLOZ67w/ieT6mCJvTLeR52Ijez6Rnp6QvxfRkCm9lXNRMUcsaQ/dZ5kMUmvER/k11eqKaFXqwVgzPrDXdz1Q1KkiS7RJTxOp1T5fi3D/i5riPX+7BsJGRm7jhGPUoj9cF3hKo8+++uvRF+p6RPqbMZt8D6NkYXCpBrcdCcwmTFmrw4s9fI177aMYZw++/H/czbEdCKhk7d5QnZPl6FreV+HA+Csd6wFrKW3tWlPy6emQdvxs6gM6zk3C09AnsVUQILWj+KWL9m+dVOkD3Txlfy+woZzeLtHFrTqvRxoRMCbvslbZ1DFwJFS2uYZozlnxMAcoIMxpU1q/vQXO1si1vNakMFficmTVF1xyYU3wDb+YuZEsFWG9UJLeF2GkQcAZo4Rgp30BaFTPBuVfDb3IM37xckssddTCw9+nieAlWti82M54CJoLkr3P9g9wMdjzM5qWxgFljO78p4/KQxv5WVkLLFgy2rI3EdzUKX86AmmNqkrBa5I8pcb8otHeBF7Lf9viCG+KZXiACwo9mNslxhyhwJjc7+7TRPMErmXXaiRUU+rVRls49TrkwpZShz8nauzaBcfrBTpzrv6uu0tddrDB4xOSFAP4eWcaaAKy9/1EKq1tw6+beDIEkJT7cdSDwMxre2a8uH4QQIfaFmnSlQYz/re9L+t0HZZTymanJCegKmgMzZk";
    private static final IseeTypologyEnum ISEE_TYPE = IseeTypologyEnum.ORDINARIO;

    @Autowired
    private CreateTokenService createTokenService;
    @Autowired
    private UserFiscalCodeService userFiscalCodeService;
    @Autowired
    private AnprInvocationService anprInvocationService;
    @Autowired
    private InpsInvocationService inpsInvocationService;

    @Autowired
    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    @BeforeEach
    void setUp() {
        onboardingDTO = OnboardingDTO.builder()
                .userId("userId_1")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .build();

        LocalDate now = LocalDate.now();
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .apiKeyClientId(ENCRYPTED_API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(ENCRYPTED_API_KEY_CLIENT_ASSERTION)
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .automatedCriteria(List.of(
                        AutomatedCriteriaDTO.builder().code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE).iseeTypes(List.of(ISEE_TYPE)).build()
                ))
                .automatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"))
                .build();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();

        RestTestUtils.USE_TRUSTSTORE_KO = true;
        BaseIntegrationTest.initServerWiremock();
    }

    @AfterEach
    void clean() {
        RestTestUtils.USE_TRUSTSTORE_KO = false;
        BaseIntegrationTest.initServerWiremock();
    }

    @Test
    void test() {

        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        Residence expectedResidence = Residence.builder()
                .postalCode("20143")
                .province("MI")
                .city("Milano")
                .build();
        BirthDate expectedBirthDate = BirthDate.builder()
                .year("1970")
                .age(Period.between(LocalDate.of(1970, 1, 1), LocalDate.now()).getYears())  // 2023-1970=53
                .build();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.valueOf(10000), result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());
    }
}
