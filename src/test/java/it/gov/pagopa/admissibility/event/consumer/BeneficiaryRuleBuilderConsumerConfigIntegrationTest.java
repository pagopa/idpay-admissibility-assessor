package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.definition.KiePackage;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl=WARN",
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
        int validRules = 100; // use even values
        int notValidRules = errorUseCases.size();
        long maxWaitingMs = 30000;

        int[] expectedRules = {0};

        List<String> initiativePayloads = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validRules / 2, expectedRules));
        initiativePayloads.addAll(IntStream.range(0, notValidRules).mapToObj(i -> errorUseCases.get(i).getFirst().get()).collect(Collectors.toList()));
        initiativePayloads.addAll(buildValidPayloads(errorUseCases.size() + (validRules / 2) + notValidRules, validRules / 2, expectedRules));

        long timeStart = System.currentTimeMillis();
        initiativePayloads.forEach(i -> publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));
        long timePublishingEnd = System.currentTimeMillis();

        long[] countSaved = {0};
        //noinspection ConstantConditions
        waitFor(() -> (countSaved[0] = droolsRuleRepository.count().block()) >= validRules, () -> "Expected %d saved rules, read %d".formatted(validRules, countSaved[0]), 15, 1000);
        long timeDroolsSavingCheckPublishingEnd = System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd = System.currentTimeMillis();

        Assertions.assertEquals(validRules, countSaved[0]);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        checkInitiativeCounters(validRules);

        checkErrorsPublished(notValidRules, maxWaitingMs);

        Mockito.verify(kieContainerBuilderServiceSpy, Mockito.atLeast(2)).buildAll(); // +1 due to refresh at startup
        Mockito.verify(onboardingContextHolderServiceSpy, Mockito.atLeast(1)).setBeneficiaryRulesKieContainer(Mockito.any());

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
        waitFor(() -> (ruleBuiltSize[0] = getRuleBuiltSize(onboardingContextHolderServiceSpy)) >= expectedRules, () -> "Expected %d rules, read %d".formatted(expectedRules, ruleBuiltSize[0]), 20, 500);
        return ruleBuiltSize[0];
    }

    public static int getRuleBuiltSize(OnboardingContextHolderService onboardingContextHolderServiceSpy) {
        KieContainer kieContainer = onboardingContextHolderServiceSpy.getBeneficiaryRulesKieContainer();
        if (kieContainer == null) {
            return 0;
        } else {
            KiePackage kiePackage = kieContainer.getKieBase().getKiePackage(KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE);
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
                        .filter(i -> i.getOnboarded() != 0L && !i.getReservedInitiativeBudget().equals(BigDecimal.ZERO))
                        .collectList()
                        .block()
        );
    }

    private final Pattern errorUseCaseIdPatternMatch = Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");

    private void checkErrorsPublished(int notValidRules, long maxWaitingMs) {
        final List<ConsumerRecord<String, String>> errors = consumeMessages(topicErrors, notValidRules, maxWaitingMs);
        for (final ConsumerRecord<String, String> record : errors) {
            final Matcher matcher = errorUseCaseIdPatternMatch.matcher(record.value());
            int useCaseId = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
            if (useCaseId == -1) {
                throw new IllegalStateException("UseCaseId not recognized! " + record.value());
            }
            errorUseCases.get(useCaseId).getSecond().accept(record);
        }
    }

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", useCaseJsonNotExpected)
        ));
        
        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", jsonNotValid)
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
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred handling initiative", criteriaCodeNotValid)
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
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred handling initiative", droolRuleSaveInError)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String s, String expectedPayload) {
        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(topicBeneficiaryRuleConsumer, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TOPIC));
        Assertions.assertNotNull(errorMessage.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals(s, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_DESCRIPTION));
        Assertions.assertEquals(errorMessage.value(), expectedPayload);
    }
    //endregion
}
