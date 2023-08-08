package it.gov.pagopa.admissibility.service.commands.operations;

import com.mongodb.MongoException;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
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
    @Mock private OnboardingContextHolderService onboardingContextHolderService;

    private DeleteInitiativeService deleteInitiativeService;

    @BeforeEach
    void setUp() {
        deleteInitiativeService = new DeleteInitiativeServiceImpl(
                droolsRuleRepositoryMock,
                initiativeCountersRepositoryMock,
                onboardingFamiliesRepositoryMock,
                auditUtilitiesMock, onboardingContextHolderService);
    }

    @Test
    void executeOK() {
        String initiativeId = "INITIATIVEID";
        String familyid = "FAMILYID";

        Mockito.when(droolsRuleRepositoryMock.deleteById(initiativeId))
                .thenReturn(Mono.just(Mockito.mock(Void.class)));

        Mockito.when(initiativeCountersRepositoryMock.deleteById(initiativeId))
                .thenReturn(Mono.just(Mockito.mock(Void.class)));

        Family family = Family.builder()
                .familyId(familyid)
                .build();

        OnboardingFamilies onboardingFamilies = OnboardingFamilies.builder(family, initiativeId).build();

        Mockito.when(onboardingFamiliesRepositoryMock.deleteByInitiativeId(initiativeId))
                .thenReturn(Flux.just(onboardingFamilies));

        String result = deleteInitiativeService.execute(initiativeId).block();

        Assertions.assertNotNull(result);

        Mockito.verify(droolsRuleRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(initiativeCountersRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(onboardingFamiliesRepositoryMock, Mockito.times(1)).deleteByInitiativeId(Mockito.anyString());
    }

    @Test
    void executeError() {
        String initiativeId = "INITIATIVEID";
        Mockito.when(droolsRuleRepositoryMock.deleteById(initiativeId))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try{
            deleteInitiativeService.execute(initiativeId).block();
            Assertions.fail();
        }catch (Throwable t){
            Assertions.assertTrue(t instanceof  MongoException);
        }
    }
}