package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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

        OnboardingDroolsDTO req = onboarding2OnboardingDroolsMapper.apply(onboardingRequest);

        if(checkIfKieBaseShouldBeInvolved(initiative)) {
            if (checkIfKieBaseContainerIsReady(initiative)) {
                StatelessKieSession statelessKieSession = onboardingContextHolderService.getBeneficiaryRulesKieBase().newStatelessKieSession();

                List<Command<?>> cmds = new ArrayList<>();
                        cmds.add(CommandFactory.newInsert(req));
                        cmds.add(CommandFactory.newInsert(criteriaCodeService));
                        cmds.add(new AgendaGroupSetFocusCommand(req.getInitiativeId()));

                long before = System.currentTimeMillis();
                statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));

                PerformanceLogger.logTiming("ONBOARDING_RULE_ENGINE", before, "resulted into rejections %s".formatted(req.getOnboardingRejectionReasons()));
            } else {
                req.getOnboardingRejectionReasons().add(new OnboardingRejectionReason(
                        OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR,
                        OnboardingConstants.REJECTION_REASON_RULE_ENGINE_NOT_READY,
                        null, null, null
                ));
            }
        } else {
            log.info("[ONBOARDING_REQUEST][RULE_ENGINE] Selected not drools involved initiative: {}", initiative.getInitiativeId());
        }

        log.trace("[ONBOARDING_REQUEST] [RULE_ENGINE] Send message prepared: {}", req);

        return onboarding2EvaluationMapper.apply(req, initiative, req.getOnboardingRejectionReasons());
    }

    private boolean checkIfKieBaseShouldBeInvolved(InitiativeConfig initiative) {
        return !CollectionUtils.isEmpty(initiative.getAutomatedCriteria()); // the drools container is supposed to not be involved
    }

    private boolean checkIfKieBaseContainerIsReady(InitiativeConfig initiative) {
        return onboardingContextHolderService.getBeneficiaryRulesKieInitiativeIds() // the initiative is inside the container drools
                        .contains(initiative.getInitiativeId());
    }
}
