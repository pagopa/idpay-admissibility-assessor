package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
import it.gov.pagopa.admissibility.utils.RestTestUtils;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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


    @BeforeEach
    void setUp() {


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

       // OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, );
    }
}
