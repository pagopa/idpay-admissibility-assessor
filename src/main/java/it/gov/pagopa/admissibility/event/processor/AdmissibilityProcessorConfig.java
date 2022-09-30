package it.gov.pagopa.admissibility.event.processor;


import it.gov.pagopa.admissibility.service.AdmissibilityEvaluatorMediatorService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class AdmissibilityProcessorConfig implements ApplicationListener<OnboardingContextHolderServiceImpl.OnboardingContextHolderReadyEvent> {

    public static final String ADMISSIBILITY_PROCESSOR_BINDING_NAME = "admissibilityProcessor-in-0";
    private final BindingsLifecycleController bindingsLifecycleController;
    private final AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorService;

    private boolean contextReady = false;

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
        if (event.getSource() instanceof Binding<?> binding && ADMISSIBILITY_PROCESSOR_BINDING_NAME.equals(binding.getBindingName()) && contextReady) {
            synchronized (this) {
                binding.start();
            }
        }
    }

    @Override
    public void onApplicationEvent(OnboardingContextHolderServiceImpl.OnboardingContextHolderReadyEvent event) {
        if(!contextReady) {
            synchronized (this) {
                contextReady = true;
                bindingsLifecycleController.changeState(ADMISSIBILITY_PROCESSOR_BINDING_NAME, BindingsLifecycleController.State.STARTED);
            }
        }
    }

}
