package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class RuleEngineServiceImpl implements RuleEngineService {
    private final OnboardingContextHolderService onboardingContextHolderService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;
    private final CriteriaCodeService criteriaCodeService;
    private final Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper;

    public RuleEngineServiceImpl(OnboardingContextHolderService onboardingContextHolderService, Onboarding2EvaluationMapper onboarding2EvaluationMapper, CriteriaCodeService criteriaCodeService, Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.criteriaCodeService = criteriaCodeService;
        this.onboarding2OnboardingDroolsMapper = onboarding2OnboardingDroolsMapper;
    }

    @Override
    public EvaluationDTO applyRules(OnboardingDTO onboardingRequest, InitiativeConfig initiative) {
        log.trace("[ONBOARDING_REQUEST] [RULE_ENGINE] evaluating rules of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        StatelessKieSession statelessKieSession = onboardingContextHolderService.getBeneficiaryRulesKieContainer().newStatelessKieSession();

        OnboardingDroolsDTO req = onboarding2OnboardingDroolsMapper.apply(onboardingRequest);

        @SuppressWarnings("unchecked")
        List<Command<?>> cmds = Arrays.asList(
                CommandFactory.newInsert(req),
                CommandFactory.newInsert(criteriaCodeService),
                new AgendaGroupSetFocusCommand(req.getInitiativeId())
        );

        long before = System.currentTimeMillis();
        statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
        long after = System.currentTimeMillis();
        log.debug("[ONBOARDING_REQUEST] Time between before and after fireAllRules: {} ms", after - before);

        log.trace("[ONBOARDING_REQUEST] [RULE_ENGINE] Send message prepared: {}", req);

        return onboarding2EvaluationMapper.apply(req, initiative, req.getOnboardingRejectionReasons());
    }
}
