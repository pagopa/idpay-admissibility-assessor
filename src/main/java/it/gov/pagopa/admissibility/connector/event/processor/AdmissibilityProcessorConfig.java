package it.gov.pagopa.admissibility.connector.event.processor;


import it.gov.pagopa.admissibility.service.onboarding.AdmissibilityEvaluatorMediatorService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class AdmissibilityProcessorConfig implements ApplicationListener<OnboardingContextHolderServiceImpl.OnboardingContextHolderReadyEvent> {

    public static final String ADMISSIBILITY_PROCESSOR_BINDING_NAME = "admissibilityProcessor-in-0";
    private final BindingsLifecycleController bindingsLifecycleController;
    private final AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService;

    private boolean contextReady = false;
    private Binding<?> admissibilityAssessorConsumerBinding;

    public AdmissibilityProcessorConfig(BindingsLifecycleController bindingsLifecycleController, AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService) {
        this.bindingsLifecycleController = bindingsLifecycleController;
        this.admissibilityEvaluatorMediatorService = admissibilityEvaluatorMediatorService;
    }

    /**
     * Read from the topic ${KAFKA_TOPIC_ONBOARDING} and publish to topic ${KAFKA_TOPIC_ONBOARDING_OUTCOME}
     */
    @Bean
    public Consumer<Flux<Message<String>>> admissibilityProcessor() {
        return this.admissibilityEvaluatorMediatorService::execute;
    }

    @EventListener(BindingCreatedEvent.class)
    public void onBindingCreatedEvent(BindingCreatedEvent event) {
        if (event.getSource() instanceof Binding<?> binding && ADMISSIBILITY_PROCESSOR_BINDING_NAME.equals(binding.getBindingName())) {
            admissibilityAssessorConsumerBinding = binding;

            if (contextReady) {
                log.info("[BENEFICIARY_CONTEXT_START] Application started and context ready");
                synchronized (this) {
                    makeServiceBusBindingRestartable(binding);
                    binding.start();
                }
            } else {
                log.info("[BENEFICIARY_CONTEXT_START] Application started but context not ready");
            }
        }
    }

    /*
     * Only setting "group" property makes the binding restartable.
     * We are using a queue, and group is a configuration valid just for topics, so we cannot configure it.
     * Because we are setting the auto-startup to false, we are not more able to start it when the container is ready without changing this flag
     */
    @SuppressWarnings("squid:S3011") // suppressing reflection accesses
    private static void makeServiceBusBindingRestartable(Binding<?> binding) {
        try {
            Field restartableField = ReflectionUtils.findField(binding.getClass(), "restartable");
            if (restartableField == null) {
                throw new IllegalStateException("Cannot make servicebus binding restartable");
            }

            restartableField.setAccessible(true);
            restartableField.setBoolean(binding, true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot make servicebus binding restartable", e);
        }
    }

    @Override
    public void onApplicationEvent(@NonNull OnboardingContextHolderServiceImpl.OnboardingContextHolderReadyEvent event) {
        if (!contextReady) {
            synchronized (this) {
                contextReady = true;
                log.info("[BENEFICIARY_CONTEXT_START] Context ready! Setting consumer as ready to start");
                if (admissibilityAssessorConsumerBinding != null) {
                    makeServiceBusBindingRestartable(admissibilityAssessorConsumerBinding);
                }
                bindingsLifecycleController.changeState(ADMISSIBILITY_PROCESSOR_BINDING_NAME, BindingsLifecycleController.State.STARTED);
            }
        }
    }

}
