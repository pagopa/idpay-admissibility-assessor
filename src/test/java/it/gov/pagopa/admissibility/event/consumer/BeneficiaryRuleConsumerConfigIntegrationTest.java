package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderService;
import it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.definition.KiePackage;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "app.beneficiary-rule.cache.refresh-ms-rate:60000",
        "logging.level.it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl=DEBUG",
})
public class BeneficiaryRuleConsumerConfigIntegrationTest extends BaseIntegrationTest {

    @SpyBean
    private KieContainerBuilderService kieContainerBuilderServiceSpy;
    @SpyBean
    private OnboardingContextHolderService onboardingContextHolderServiceSpy;

    @Test
    void testBeneficiaryRuleBuilding(){
        int N=100;
        int[] expectedRules ={0};
        List<Initiative2BuildDTO> initiatives = IntStream.range(0,N)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .peek(i->expectedRules[0]+=i.getBeneficiaryRule().getAutomatedCriteria().size())
                .toList();

        long timeStart=System.currentTimeMillis();
        initiatives.forEach(i->publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));
        long timePublishingEnd=System.currentTimeMillis();

        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=droolsRuleRepository.count().block()) >= N, ()->"Expected %d saved rules, read %d".formatted(N, countSaved[0]), 15, 1000);
        long timeDroolsSavingCheckPublishingEnd=System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(N, countSaved[0]);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        Mockito.verify(kieContainerBuilderServiceSpy, Mockito.atLeast(2)).buildAll(); // +1 due to refresh at startup
        Mockito.verify(onboardingContextHolderServiceSpy, Mockito.atLeast(1)).setBeneficiaryRulesKieContainer(Mockito.any());

        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Time spent to assert drools rule count (from previous check): %d millis
            Time spent to assert kie container rules' size (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            The kieContainer has been built %d times
            ************************
            """,
                N,
                timePublishingEnd-timeStart,
                timeDroolsSavingCheckPublishingEnd-timePublishingEnd,
                timeEnd-timeDroolsSavingCheckPublishingEnd,
                timeEnd-timeStart,
                Mockito.mockingDetails(kieContainerBuilderServiceSpy).getInvocations().stream()
                        .filter(i->i.getMethod().getName().equals("buildAll")).count()-1 // 1 is due on startup
        );
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

}
