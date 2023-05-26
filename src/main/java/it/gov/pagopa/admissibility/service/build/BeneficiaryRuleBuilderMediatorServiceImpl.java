package it.gov.pagopa.admissibility.service.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.BaseKafkaConsumer;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class BeneficiaryRuleBuilderMediatorServiceImpl extends BaseKafkaConsumer<Initiative2BuildDTO, DroolsRule> implements BeneficiaryRuleBuilderMediatorService {
    private final Duration commitDelay;
    private final Duration beneficiaryRulesBuildDelayMinusCommit;
    private final BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final OnboardingContextHolderService onboardingContextHolderService;
    private final InitInitiativeCounterService initInitiativeCounterService;
    private final AdmissibilityErrorNotifierService admissibilityErrorNotifierService;
    private final BeneficiaryRuleFilterService beneficiaryRuleFilterService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public BeneficiaryRuleBuilderMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.beneficiaryRuleBuilderConsumer-in-0.consumer.ackTime}") long commitMillis,
            @Value("${app.beneficiary-rule.build-delay-duration}") String beneficiaryRulesBuildDelay,

            BeneficiaryRule2DroolsRule beneficiaryRule2DroolsRule,
            DroolsRuleRepository droolsRuleRepository,
            KieContainerBuilderService kieContainerBuilderService,
            OnboardingContextHolderService onboardingContextHolderService,
            InitInitiativeCounterService initInitiativeCounterService,
            AdmissibilityErrorNotifierService admissibilityErrorNotifierService,
            BeneficiaryRuleFilterService beneficiaryRuleFilterService,

            ObjectMapper objectMapper) {
        super(applicationName);

        this.commitDelay = Duration.ofMillis(commitMillis);

        Duration beneficiaryRulesBuildDelayDuration = Duration.parse(beneficiaryRulesBuildDelay).minusMillis(commitMillis);
        Duration defaultDurationDelay = Duration.ofMillis(2L);
        this.beneficiaryRulesBuildDelayMinusCommit = defaultDurationDelay.compareTo(beneficiaryRulesBuildDelayDuration) >= 0 ? defaultDurationDelay : beneficiaryRulesBuildDelayDuration;

        this.beneficiaryRule2DroolsRule = beneficiaryRule2DroolsRule;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.initInitiativeCounterService = initInitiativeCounterService;
        this.admissibilityErrorNotifierService = admissibilityErrorNotifierService;
        this.beneficiaryRuleFilterService = beneficiaryRuleFilterService;

        this.objectReader = objectMapper.readerFor(Initiative2BuildDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<DroolsRule>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(beneficiaryRulesBuildDelayMinusCommit)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(onboardingContextHolderService::setBeneficiaryRulesKieBase);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> admissibilityErrorNotifierService.notifyBeneficiaryRuleBuilder(message, "[ADMISSIBILITY_RULE_BUILD] Unexpected JSON", true, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        admissibilityErrorNotifierService.notifyBeneficiaryRuleBuilder(message, "[ADMISSIBILITY_RULE_BUILD] An error occurred handling initiative", true, e);
    }

    @Override
    protected Mono<DroolsRule> execute(Initiative2BuildDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .filter(this.beneficiaryRuleFilterService::filter)
                .map(beneficiaryRule2DroolsRule)
                .flatMap(droolsRuleRepository::save)
                .doOnNext(i -> {
                    onboardingContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    onboardingContextHolderService.setPDNDapiKeys(i.getInitiativeConfig());
                })
                .flatMap(this::initializeCounters);
    }

    private Mono<DroolsRule> initializeCounters(DroolsRule droolsRule) {
        return initInitiativeCounterService.initCounters(droolsRule.getInitiativeConfig())
                .then(Mono.just(droolsRule));
    }

    @Override
    public String getFlowName() {
        return "ADMISSIBILITY_RULE_BUILD";
    }
}
