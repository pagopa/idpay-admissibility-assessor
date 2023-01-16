package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
import it.gov.pagopa.admissibility.utils.RestTestUtils;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

class AuthoritiesDataRetrieverServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CreateTokenService createTokenService;
    @Autowired
    private UserFiscalCodeService userFiscalCodeService;
    @Autowired
    private ResidenceAssessmentService residenceAssessmentService;
    @Autowired
    private TipoResidenzaDTO2ResidenceMapper residenceMapper;

    @Autowired
    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    @BeforeEach
    void setUp() {
        onboardingDTO = OnboardingDTO.builder()
                .userId("USERID_OK_1")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .build();

        LocalDate now = LocalDate.now();
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .pdndToken("PDND_TOKEN")
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
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
                .age(Period.between(LocalDate.of(1970,1,1), LocalDate.now()).getYears())  // 2023-1970=53
                .build();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.valueOf(24428), result.getIsee());   // TODO INPS
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());
    }
}
