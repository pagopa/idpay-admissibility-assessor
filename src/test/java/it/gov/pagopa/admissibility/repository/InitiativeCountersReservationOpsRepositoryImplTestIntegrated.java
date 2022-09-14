package it.gov.pagopa.admissibility.repository;

import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
class InitiativeCountersReservationOpsRepositoryImplTestIntegrated extends InitiativeCountersReservationOpsRepositoryImplTest {
    @AfterEach
    void clearData(){
        initiativeCountersRepository.deleteById("prova").block();
    }
}
