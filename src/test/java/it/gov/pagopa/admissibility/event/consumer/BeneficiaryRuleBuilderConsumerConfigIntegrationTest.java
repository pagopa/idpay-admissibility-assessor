package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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

    @Test
    void testBeneficiaryRuleBuilding(){
        int validRules=100; // use even values
        int notValidRules=errorUseCases.size();
        long maxWaitingMs = 30000;

        int[] expectedRules ={0};

        List<String> initiativePayloads = new ArrayList<>(buildValidPayloads(0, validRules/2, expectedRules));
        initiativePayloads.addAll(IntStream.range(0,notValidRules).mapToObj(i->errorUseCases.get(i).getFirst().apply(i)).collect(Collectors.toList()));
        initiativePayloads.addAll(buildValidPayloads((validRules/2) + notValidRules, validRules/2, expectedRules));

        long timeStart=System.currentTimeMillis();
        initiativePayloads.forEach(i->publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));
        long timePublishingEnd=System.currentTimeMillis();

        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=droolsRuleRepository.count().block()) >= validRules, ()->"Expected %d saved rules, read %d".formatted(validRules, countSaved[0]), 15, 1000);
        long timeDroolsSavingCheckPublishingEnd=System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(validRules, countSaved[0]);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        checkInitiativeCounters(validRules);

        final List<ConsumerRecord<String, String>> errors = consumeMessages(topicErrors, notValidRules, maxWaitingMs);
        for (int i = 0; i < errors.size(); i++) {
            errorUseCases.get(i).getSecond().accept(errors.get(i));
        }

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
                validRules+notValidRules,
                validRules,
                notValidRules,
                timePublishingEnd-timeStart,
                timeDroolsSavingCheckPublishingEnd-timePublishingEnd,
                timeEnd-timeDroolsSavingCheckPublishingEnd,
                timeEnd-timeStart,
                Mockito.mockingDetails(kieContainerBuilderServiceSpy).getInvocations().stream()
                        .filter(i->i.getMethod().getName().equals("buildAll")).count()-1 // 1 is due on startup
        );
    }

    private List<String> buildValidPayloads(int bias, int validRules, int[] expectedRules) {
        return IntStream.range(bias, bias + validRules)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .peek(i->expectedRules[0]+=i.getBeneficiaryRule().getAutomatedCriteria().size())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private int waitForKieContainerBuild(int expectedRules) {return waitForKieContainerBuild(expectedRules, onboardingContextHolderServiceSpy);}

    public static int waitForKieContainerBuild(int expectedRules,OnboardingContextHolderService onboardingContextHolderServiceSpy) {
        int[] ruleBuiltSize={0};
        waitFor(()->(ruleBuiltSize[0]=getRuleBuiltSize(onboardingContextHolderServiceSpy)) >= expectedRules, ()->"Expected %d rules, read %d".formatted(expectedRules, ruleBuiltSize[0]), 20, 500);
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
                        .filter(i->i.getOnboarded()!=0L && !i.getReservedInitiativeBudget().equals(BigDecimal.ZERO))
                        .collectList()
                        .block()
        );
    }

    //region not valid useCases
    private final List<Pair<Function<Integer, String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = List.of(
            // Json not expected
            Pair.of(
                    i -> "{unexpectedStructure:0}",
                    errorMessage -> {
                        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_SERVER));
                        Assertions.assertEquals(topicBeneficiaryRuleConsumer, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TOPIC));
                        Assertions.assertNotNull(errorMessage.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_STACKTRACE));
                        Assertions.assertEquals("Unexpected JSON", TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_DESCRIPTION));
                        Assertions.assertEquals("{unexpectedStructure:0}", errorMessage.value());
                    }
            ),
            // Json not valid
            Pair.of(
                    i -> "{invalidJson",
                    errorMessage -> {
                        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_SERVER));
                        Assertions.assertEquals(topicBeneficiaryRuleConsumer, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TOPIC));
                        Assertions.assertNotNull(errorMessage.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_STACKTRACE));
                        Assertions.assertEquals("Unexpected JSON", TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_DESCRIPTION));
                        Assertions.assertEquals("{invalidJson", errorMessage.value());
                    }
            )
    );
    //endregion
}
