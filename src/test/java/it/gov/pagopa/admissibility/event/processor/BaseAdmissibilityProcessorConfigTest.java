package it.gov.pagopa.admissibility.event.processor;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.service.onboarding.*;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.RuleEngineService;
import it.gov.pagopa.admissibility.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.AdmissibilityEvaluatorMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.BaseKafkaConsumer=WARN",
        "logging.level.it.gov.pagopa.admissibility.utils.PerformanceLogger=WARN",
})
abstract class BaseAdmissibilityProcessorConfigTest extends BaseIntegrationTest {

    @SpyBean
    protected OnboardingContextHolderService onboardingContextHolderServiceSpy;
    @SpyBean
    protected OnboardingCheckService onboardingCheckServiceSpy;
    @SpyBean
    protected AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceSpy;
    @SpyBean
    protected RuleEngineService ruleEngineServiceSpy;
    @SpyBean
    protected InitiativeCountersRepository initiativeCountersRepositorySpy;

    protected static List<Checkpointer> checkpointers;


    static class MediatorSpyConfiguration {
        @SpyBean
        private AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorServiceSpy;

        @PostConstruct
        void init() {
            checkpointers = configureSpies();
        }

        private List<Checkpointer> configureSpies(){
            List<Checkpointer> checkpoints = Collections.synchronizedList(new ArrayList<>(1100));

            Mockito.doAnswer(args-> {
                        Flux<Message<String>> messageFlux = args.getArgument(0);
                        messageFlux = messageFlux.map(m -> {
                                    Checkpointer mock = Mockito.mock(Checkpointer.class);
                                    Mockito.when(mock.success()).thenReturn(Mono.empty());
                                    checkpoints.add(mock);
                                    return MessageBuilder.withPayload(m.getPayload())
                                            .copyHeaders(m.getHeaders())
                                            .setHeader(AzureHeaders.CHECKPOINTER, mock)
                                            .build();
                                    }
                                )
                                .name("spy");
                        admissibilityEvaluatorMediatorServiceSpy.execute(messageFlux);
                        return null;
                    })
                    .when(admissibilityEvaluatorMediatorServiceSpy).execute(Mockito.argThat(a -> !Scannable.from(a).name().equals("spy")));

            return  checkpoints;
        }
    }


    protected <T extends EvaluationDTO> List<String> buildValidPayloads(int bias, int validOnboardings,List<Pair<Function<Integer, OnboardingDTO>, Consumer<T>>> useCases) {
        return IntStream.range(bias, bias + validOnboardings)
                .mapToObj(i -> this.mockInstance(i,useCases))
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    protected <T extends EvaluationDTO> OnboardingDTO mockInstance(int bias, List<Pair<Function<Integer, OnboardingDTO>, Consumer<T>>> useCases) {
        return useCases.get(bias % useCases.size()).getFirst().apply(bias);
    }

    protected <T extends EvaluationDTO> void checkResponse(T evaluation, List<Pair<Function<Integer, OnboardingDTO>, Consumer<T>>> useCases) {
        String userId = evaluation.getUserId();
        int biasRetrieve = Integer.parseInt(userId.substring(userId.indexOf("userId_") + 7));
        int useCaseIndex = biasRetrieve % useCases.size();
        try {
            useCases.get(useCaseIndex).getSecond().accept(evaluation);
        } catch (Throwable e) {
            System.err.printf("Failed use case %d on user %s initiativeId %s%n",
                    useCaseIndex,
                    userId,
                    evaluation.getInitiativeId());
            throw e;
        }
    }

    protected void checkOffsets(long expectedReadMessages, long exptectedPublishedResults, String outputTopic) {
        Assertions.assertEquals(expectedReadMessages, checkpointers.size());
        checkpointers.forEach(checkpointer -> Mockito.verify(checkpointer).success());

        long timeCommitChecked = System.currentTimeMillis();
        final Map<TopicPartition, Long> destPublishedOffsets = checkPublishedOffsets(outputTopic, exptectedPublishedResults);
        long timePublishChecked = System.currentTimeMillis();
        System.out.printf("""
                        ************************
                        Time occurred to check published offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        Dest Topic Published Offsets: %s
                        ************************
                        """,
                timePublishChecked - timeCommitChecked,
                expectedReadMessages,
                destPublishedOffsets
        );
    }
}