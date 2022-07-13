package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RuleEngineServiceImpl implements RuleEngineService {
    private final OnboardingContextHolderService onboardingContextHolderService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;
    private final Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper;

    public RuleEngineServiceImpl(OnboardingContextHolderService onboardingContextHolderService, Onboarding2EvaluationMapper onboarding2EvaluationMapper, Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.onboarding2OnboardingDroolsMapper = onboarding2OnboardingDroolsMapper;
    }

    @Override
    public EvaluationDTO applyRules(OnboardingDTO onboardingDTO) {

        StatelessKieSession statelessKieSession = onboardingContextHolderService.getKieContainer().newStatelessKieSession();

        List<Command> cmds = new ArrayList<>();
        OnboardingDroolsDTO req = onboarding2OnboardingDroolsMapper.apply(onboardingDTO);
        cmds.add(CommandFactory.newInsert(req));

        cmds.add(new AgendaGroupSetFocusCommand(onboardingDTO.getInitiativeId()));

        Instant before = Instant.now();
        statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
        Instant after = Instant.now();
        log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());

        log.info("Send message prepared: {}", onboardingDTO);

        return onboarding2EvaluationMapper.apply(req, req.getOnboardingRejectionReasons());
    }
}
