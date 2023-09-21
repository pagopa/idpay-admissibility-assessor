package it.gov.pagopa.admissibility.connector.event.consumer;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl=WARN",
        "logging.level.it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer=WARN",
        "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
})
public class BeneficiaryRuleBuilderConsumerConfigIntegrationTest extends BaseIntegrationTest {

    @SpyBean
    private KieContainerBuilderService kieContainerBuilderServiceSpy;
    @SpyBean
    private OnboardingContextHolderService onboardingContextHolderServiceSpy;
    @SpyBean
    private DroolsRuleRepository droolsRuleRepositorySpy;

    @Test
    void testBeneficiaryRuleBuilding() {
        int validRules = 100; // use even number
        int notValidRules = errorUseCases.size();
        long maxWaitingMs = 40000;

        int[] expectedRules = {0};

        List<String> initiativePayloads = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validRules / 2, expectedRules));
        initiativePayloads.addAll(IntStream.range(0, notValidRules).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        initiativePayloads.addAll(buildValidPayloads(errorUseCases.size() + (validRules / 2) + notValidRules, validRules / 2, expectedRules));

        long timeStart = System.currentTimeMillis();
        initiativePayloads.forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, List.of(new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingEnd = System.currentTimeMillis();

        long[] countSaved = {0};
        //noinspection ConstantConditions
        TestUtils.waitFor(() -> (countSaved[0] = droolsRuleRepository.count().block()) >= validRules, () -> "Expected %d saved rules, read %d".formatted(validRules, countSaved[0]), 15, 1000);
        long timeDroolsSavingCheckPublishingEnd = System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd = System.currentTimeMillis();

        Assertions.assertEquals(validRules, countSaved[0]);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        checkInitiativeCounters(validRules);

        checkErrorsPublished(notValidRules, maxWaitingMs, errorUseCases);

        Mockito.verify(kieContainerBuilderServiceSpy, Mockito.atLeast(1)).buildAll(); // +1 due to refresh at startup
        Mockito.verify(onboardingContextHolderServiceSpy, Mockito.atLeast(1)).setBeneficiaryRulesKieBase(Mockito.any());

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert drools rule count (from previous check): %d millis
                        Time spent to assert kie container rules' size (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        The kieContainer has been built %d times
                        ************************
                        """,
                validRules + notValidRules,
                validRules,
                notValidRules,
                timePublishingEnd - timeStart,
                timeDroolsSavingCheckPublishingEnd - timePublishingEnd,
                timeEnd - timeDroolsSavingCheckPublishingEnd,
                timeEnd - timeStart,
                Mockito.mockingDetails(kieContainerBuilderServiceSpy).getInvocations().stream()
                        .filter(i -> i.getMethod().getName().equals("buildAll")).count() - 1 // 1 is due on startup
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = kafkaTestUtilitiesService.checkCommittedOffsets(topicBeneficiaryRuleConsumer, groupIdBeneficiaryRuleConsumer,initiativePayloads.size()+1); // +1 due to other applicationName useCase
        long timeCommitCheckEnd = System.currentTimeMillis();
        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );
    }

    private List<String> buildValidPayloads(int bias, int validRules, int[] expectedRules) {
        return IntStream.range(bias, bias + validRules)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private int waitForKieContainerBuild(int expectedRules) {
        return waitForKieContainerBuild(expectedRules, onboardingContextHolderServiceSpy);
    }

    public static int waitForKieContainerBuild(int expectedRules, OnboardingContextHolderService onboardingContextHolderServiceSpy) {
        int[] ruleBuiltSize = {0};
        TestUtils.waitFor(() -> (ruleBuiltSize[0] = getRuleBuiltSize(onboardingContextHolderServiceSpy)) >= expectedRules, () -> "Expected %d rules, read %d".formatted(expectedRules, ruleBuiltSize[0]), 20, 1000);
        return ruleBuiltSize[0];
    }

    public static int getRuleBuiltSize(OnboardingContextHolderService onboardingContextHolderServiceSpy) {
        KieBase kieBase = onboardingContextHolderServiceSpy.getBeneficiaryRulesKieBase();
        if (kieBase == null) {
            return 0;
        } else {
            KiePackage kiePackage = kieBase.getKiePackage(KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE);
            return kiePackage != null
                    ? kiePackage.getRules().size()
                    : 0;
        }
    }

    private void checkInitiativeCounters(int expectedInitiativeNumber) {
        Assertions.assertEquals(expectedInitiativeNumber, initiativeCountersRepository.count().block());
        Assertions.assertEquals(
                Collections.emptyList(),
                initiativeCountersRepository.findAll()
                        .filter(i ->
                                i.getOnboarded() == 1L
                                        && i.getReservedInitiativeBudgetCents() > 0L
                                        && i.getResidualInitiativeBudgetCents() < i.getInitiativeBudgetCents()
                                        && i.getResidualInitiativeBudgetCents() > 0L)
                        .collectList()
                        .block()
        );
    }

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_RULE_BUILD] Unexpected JSON", useCaseJsonNotExpected)
        ));
        
        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_RULE_BUILD] Unexpected JSON", jsonNotValid)
        ));
        
        String criteriaCodeNotValid = TestUtils.jsonSerializer(Initiative2BuildDTOFaker.mockInstanceBuilder(errorUseCases.size())
                .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                        .automatedCriteria(List.of(
                                AutomatedCriteriaDTO.builder()
                                        .code("DUMMY")
                                        .build()
                        ))
                        .build()));
        errorUseCases.add(Pair.of(
                () -> criteriaCodeNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_RULE_BUILD] An error occurred handling initiative", criteriaCodeNotValid)
        ));

        final String errorWhenSavingUseCaseId = "id_%s_ERRORWHENSAVING".formatted(errorUseCases.size());
        String droolRuleSaveInError = TestUtils.jsonSerializer(Initiative2BuildDTOFaker.mockInstanceBuilder(errorUseCases.size())
                .initiativeId(errorWhenSavingUseCaseId)
                .build());
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(droolsRuleRepositorySpy).save(Mockito.argThat(i->errorWhenSavingUseCaseId.equals(i.getId())));
                    return droolRuleSaveInError;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_RULE_BUILD] An error occurred handling initiative", droolRuleSaveInError)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(kafkaBootstrapServers, topicBeneficiaryRuleConsumer, groupIdBeneficiaryRuleConsumer, errorMessage, errorDescription, expectedPayload, null, true, true);
    }
    //endregion
}
