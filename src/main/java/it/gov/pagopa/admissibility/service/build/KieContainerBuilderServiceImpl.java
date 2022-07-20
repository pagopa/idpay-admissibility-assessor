package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class KieContainerBuilderServiceImpl implements KieContainerBuilderService {

    private final DroolsRuleRepository droolsRuleRepository;

    public KieContainerBuilderServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<KieContainer> buildAll() {
        log.info("Fetching and building all the initiatives");
        return build(droolsRuleRepository.findAll());
    }

    @Override
    public Mono<KieContainer> build(Flux<DroolsRule> rules) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

        return rules.map(r -> kieFileSystem.write(String.format("src/main/resources/it/gov/pagopa/admissibility/drools/buildrules/%s.drl", r.getName()), r.getRule()))
                .then(Mono.fromSupplier(() -> {
                    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                    kieBuilder.buildAll();
                    /* TODO check and notify errors
                    if (kb.getResults().hasMessages(Message.Level.ERROR)) {
                        throw new IllegalArgumentException("Build Errors:" + kb.getResults().toString());
                    }*/
                    KieModule kieModule = kieBuilder.getKieModule();

                    log.info("Build completed");
                    return kieServices.newKieContainer(kieModule.getReleaseId());
                }));
    }
}
