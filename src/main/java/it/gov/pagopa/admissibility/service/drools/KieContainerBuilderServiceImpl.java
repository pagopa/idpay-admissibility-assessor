package it.gov.pagopa.admissibility.service.drools;

import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class KieContainerBuilderServiceImpl implements KieContainerBuilderService{

    private final DroolsRuleRepository droolsRuleRepository;

    public KieContainerBuilderServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<KieContainer> append(DroolsRule droolsRule) {
        return null; // TODO
    }

    @Override
    public KieContainer buildAll() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

        Flux<DroolsRule> beneficiaryRules = droolsRuleRepository.findAll();
        beneficiaryRules.subscribe(r ->   kieFileSystem.write(String.format("src/main/resources/templateRules/%s.drl",r.getName()),applyRuleTemplate(r)));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    public static String applyRuleTemplate(DroolsRule rule) {
       Map<String, Object> data = new HashMap<>();
       ObjectDataCompiler objectDataCompiler = new ObjectDataCompiler();

       data.put("name", rule.getName());
       data.put("agenda", rule.getAgendaGroup());
       data.put("condition", rule.getRuleCondition());
       data.put("consequence", rule.getRuleConsequence());

       return objectDataCompiler.compile(
               Collections.singletonList(data),
               // The stream will be read as UTF-8
               Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/drools/template/template.drl")));
    }
}
