package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
    private static final IseeTypologyEnum ISEE_TYPE = IseeTypologyEnum.ORDINARIO;

    @Autowired
    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    @BeforeAll
    static void configureWiremock() {
        BaseIntegrationTest.configureServerWiremockBeforeAll(false, false);
    }

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
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .automatedCriteria(List.of(
                        AutomatedCriteriaDTO.builder().code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE).iseeTypes(List.of(ISEE_TYPE)).build()
                ))
                .automatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"))
                .build();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();
    }

    @Test
    void test() {
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        Residence expectedResidence = Residence.builder()
                .postalCode("41026")
                .province("MO")
                .city("PAVULLO NEL FRIGNANO")
                .cityCouncil("PAVULLO NEL FRIGNANO")
                .build();
        BirthDate expectedBirthDate = BirthDate.builder()
                .year("1990")
                .age(Period.between(LocalDate.of(1990, 1, 1), LocalDate.now()).getYears())  // 2023-1970=53
                .build();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.valueOf(10000), result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());
    }
}
