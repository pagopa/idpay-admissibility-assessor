package it.gov.pagopa.admissibility.service.drools;

import it.gov.pagopa.admissibility.model.DroolsRule;
import org.kie.api.runtime.KieContainer;
import reactor.core.publisher.Mono;

/**
 * It will handle the compilation operation to obtain a new KieContainer
 * */
public interface KieContainerBuilderService {
    /**
     * It will take {@link DroolsRule}, build it, append it to all the previous rule of KieContainer*/
    Mono<KieContainer> append(DroolsRule droolsRule);
    /**
     * It will fetch all the {@link DroolsRule} entity , build and new KieContainer*/
    KieContainer buildAll();
}
