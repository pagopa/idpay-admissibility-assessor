package it.gov.pagopa.admissibility.service.commands.operations;

import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private InitiativeCountersRepository initiativeCountersRepositoryMock;
    @Mock private OnboardingFamiliesRepository onboardingFamiliesRepositoryMock;
    @Mock private AuditUtilities auditUtilitiesMock;

    private DeleteInitiativeService deleteInitiativeService;
    private final static int PAGE_SIZE = 100;


    @BeforeEach
    void setUp() {
        deleteInitiativeService = new DeleteInitiativeServiceImpl(
                droolsRuleRepositoryMock,
                initiativeCountersRepositoryMock,
                onboardingFamiliesRepositoryMock,
                auditUtilitiesMock,
                PAGE_SIZE, 1000L);
    }

    @Test
    void executeOK() {
        String initiativeId = "INITIATIVEID";
        String familyid = "FAMILYID";

        Mockito.when(droolsRuleRepositoryMock.removeById(initiativeId))
                .thenReturn(Mono.just(Mockito.mock(DeleteResult.class)));

        Mockito.when(initiativeCountersRepositoryMock.removeById(initiativeId))
                .thenReturn(Mono.just(Mockito.mock(DeleteResult.class)));

        Family family = Family.builder()
                .familyId(familyid)
                .build();

        OnboardingFamilies onboardingFamilies = OnboardingFamilies.builder(family, initiativeId).build();

        Mockito.when(onboardingFamiliesRepositoryMock.findByInitiativeIdWithBatch(initiativeId,PAGE_SIZE))
                .thenReturn(Flux.just(onboardingFamilies));

        Mockito.when(onboardingFamiliesRepositoryMock.deleteById(onboardingFamilies.getId()))
                .thenReturn(Mono.empty());


        String result = deleteInitiativeService.execute(initiativeId).block();

        Assertions.assertNotNull(result);

        Mockito.verify(droolsRuleRepositoryMock, Mockito.times(1)).removeById(Mockito.anyString());
        Mockito.verify(initiativeCountersRepositoryMock, Mockito.times(1)).removeById(Mockito.anyString());
        Mockito.verify(onboardingFamiliesRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(),Mockito.anyInt());
        Mockito.verify(onboardingFamiliesRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());

    }

    @Test
    void executeError() {
        String initiativeId = "INITIATIVEID";
        Mockito.when(droolsRuleRepositoryMock.removeById(initiativeId))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try {
            deleteInitiativeService.execute(initiativeId).block();
        }catch (Exception e){

            Assertions.assertTrue(e instanceof MongoException);
        }
    }
}