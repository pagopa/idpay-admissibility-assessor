package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.repository.CustomSequenceGeneratorRepository;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.CustomSequenceGenerator;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
//@TestPropertySource(locations = {"classpath:/secrets/pdndConfig.properties"})
@ContextConfiguration(inheritInitializers = false)
class AnprC001RestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private CustomSequenceGeneratorRepository sequenceGeneratorRepository;

    @Autowired
    private AnprC001RestClient anprC001RestClient;

    @Autowired
    private PdndInitiativeConfig pdndInitiativeConfig;

    @Test
    void getResidenceAssessment(){
        // Given
        String fiscalCode = "STTSGT90A01H501J";
        int sequenceVal = 0; // if ANPR will start to validate the sequence, we need to set this to the actual sequence value stored in UAT (which should be updated once terminated)

        sequenceGeneratorRepository.save(new CustomSequenceGenerator(OnboardingConstants.ANPR_E002_INVOKE, sequenceVal)).block();

        // When
        RispostaE002OKDTO result = anprC001RestClient.invoke(fiscalCode, pdndInitiativeConfig).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getIdOperazioneANPR());

        Assertions.assertEquals("SOGGETTO", result.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getNome());
        Assertions.assertEquals("SETTIMO", result.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getCognome());

        Assertions.assertEquals("1990-01-01", result.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getDataNascita());

        Assertions.assertEquals("41026", result.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0).getIndirizzo().getCap());
        Assertions.assertEquals("036030", result.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0).getIndirizzo().getComune().getCodiceIstat());
        Assertions.assertEquals("PAVULLO NEL FRIGNANO", result.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0).getIndirizzo().getComune().getNomeComune());
        Assertions.assertEquals("MO", result.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0).getIndirizzo().getComune().getSiglaProvinciaIstat());
    }
}