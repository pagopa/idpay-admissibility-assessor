package it.gov.pagopa.admissibility.connector.repository;

import it.gov.pagopa.common.mongo.MongoTestIntegrated;
import org.junit.jupiter.api.AfterEach;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@MongoTestIntegrated
class InitiativeCountersReservationOpsRepositoryImplTestIntegrated extends InitiativeCountersReservationOpsRepositoryImplTest {
    @AfterEach
    void clearData(){
        initiativeCountersRepository.deleteById("prova").block();
    }
}