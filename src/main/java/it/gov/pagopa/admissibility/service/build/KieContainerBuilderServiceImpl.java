package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.command.Command;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class KieContainerBuilderServiceImpl implements KieContainerBuilderService {

    public static final String RULES_BUILT_PACKAGE = "it.gov.pagopa.admissibility.drools.buildrules";
    private static final String RULES_BUILT_DIR = RULES_BUILT_PACKAGE.replace(".", "/");

    private final DroolsRuleRepository droolsRuleRepository;

    public KieContainerBuilderServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<KieBase> buildAll() {
        log.info("[BENEFICIARY_RULE_BUILDER] Fetching and building all the initiatives");
        return build(droolsRuleRepository.findAll());
    }

    @Override
    public Mono<KieBase> build(Flux<DroolsRule> rules) {
        return Mono.defer(() -> {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

            return rules.map(r -> kieFileSystem.write(String.format("src/main/resources/%s/%s.drl", RULES_BUILT_DIR, r.getId()), r.getRule()))
                    .then(Mono.fromSupplier(() -> {
                        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                        kieBuilder.buildAll();

                        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                            throw new IllegalArgumentException("[BENEFICIARY_RULE_BUILDER] Build Errors:" + kieBuilder.getResults().toString());
                        }

                        KieModule kieModule = kieBuilder.getKieModule();
                        KieBase newKieBase = kieServices.newKieContainer(kieModule.getReleaseId()).getKieBase();

                        log.info("[BENEFICIARY_RULE_BUILDER] Build completed");
                        if (log.isDebugEnabled()) {
                            KiePackage kiePackage = newKieBase.getKiePackage(RULES_BUILT_PACKAGE);
                            log.debug("[BENEFICIARY_RULE_BUILDER] The container now will contain the following rules inside %s package: %s".formatted(
                                    RULES_BUILT_PACKAGE,
                                    kiePackage != null
                                            ? kiePackage.getRules().stream().map(Rule::getId).toList()
                                            : "0"));
                        }
                        return newKieBase;
                    }));
        });
    }

    @Override
    @SuppressWarnings({"Convert2Lambda","squid:S1604"}) // Cannot use lambda expression otherwise it should be not serializable
    public void preLoadKieBase(KieBase kieBase){
            try {
                log.info("[DROOLS_CONTAINER_COMPILE] Starting KieContainer compile");
                long startTime = System.currentTimeMillis();
                OnboardingDroolsDTO req = new OnboardingDroolsDTO();
                req.setIsee(BigDecimal.ZERO);
                req.setBirthDate(new BirthDate("1900",0));
                req.setResidence(new Residence("", "", "", "", "", ""));
                List<Command<?>> cmds = new ArrayList<>();
                cmds.add(CommandFactory.newInsert(new CriteriaCodeService() {
                    @Override
                    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {
                        return new CriteriaCodeConfig();
                    }
                }));
                cmds.add(CommandFactory.newInsert(req));
                Arrays.stream(((KnowledgeBaseImpl) kieBase).getPackages()).flatMap(p -> p.getRules().stream()).map(r -> ((RuleImpl) r).getAgendaGroup())
                        .distinct().forEach(a -> cmds.add(new AgendaGroupSetFocusCommand(a)));
                StatelessKieSession session = kieBase.newStatelessKieSession();
                session.execute(CommandFactory.newBatchExecution(cmds));
                long endTime = System.currentTimeMillis();

                log.info("[DROOLS_CONTAINER_COMPILE] KieContainer instance compiled in {} ms", endTime - startTime);
            } catch (Exception e){
                log.warn("[DROOLS_CONTAINER_COMPILE] An error occurred while pre-compiling Drools rules. This will not influence the right behavior of the application, the rules will be compiled the first time they are used", e);
            }
        }
}
