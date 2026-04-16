package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2OnboardingDroolsMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.RejectionReasonService;
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
    private final RejectionReasonService rejectionReasonService;

    public RuleEngineServiceImpl(
            OnboardingContextHolderService onboardingContextHolderService,
            Onboarding2EvaluationMapper onboarding2EvaluationMapper,
            CriteriaCodeService criteriaCodeService,
            Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper,
            RejectionReasonService rejectionReasonService) {

        this.onboardingContextHolderService = onboardingContextHolderService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.criteriaCodeService = criteriaCodeService;
        this.onboarding2OnboardingDroolsMapper = onboarding2OnboardingDroolsMapper;
        this.rejectionReasonService = rejectionReasonService;
    }

    @Override
    public EvaluationDTO applyRules(OnboardingDTO onboardingRequest,
                                    InitiativeConfig initiative) {

        log.trace(
                "[ONBOARDING_REQUEST][RULE_ENGINE] evaluating rules of user {} into initiative {}",
                onboardingRequest.getUserId(),
                onboardingRequest.getInitiativeId()
        );

        OnboardingDroolsDTO req =
                onboarding2OnboardingDroolsMapper.apply(onboardingRequest);

        for (VerifyDTO verify : onboardingRequest.getVerifies()) {

            if (!verify.isBlockingVerify()) {
                continue;
            }

            for (ResultVerifyDTO resultVerify : onboardingRequest.getResultsVerifies()) {

                if (verify.getCode().equals(resultVerify.getCode())
                        && !resultVerify.isResultVerify()) {

                    log.debug(
                            "[ONBOARDING_REQUEST][BLOCKING_VERIFY_KO] code={}",
                            verify.getCode()
                    );

                    req.getOnboardingRejectionReasons()
                            .add(rejectionReasonService.rejectionFor(verify.getCode()));

                    return onboarding2EvaluationMapper.apply(
                            req,
                            initiative,
                            req.getOnboardingRejectionReasons()
                    );
                }
            }
        }

        if (checkIfKieBaseShouldBeInvolved(initiative)) {

            if (checkIfKieBaseContainerIsReady(initiative)) {

                StatelessKieSession statelessKieSession =
                        onboardingContextHolderService
                                .getBeneficiaryRulesKieBase()
                                .newStatelessKieSession();

                List<Command<?>> cmds = new ArrayList<>();
                cmds.add(CommandFactory.newInsert(req));
                cmds.add(CommandFactory.newInsert(criteriaCodeService));
                cmds.add(new AgendaGroupSetFocusCommand(req.getInitiativeId()));

                long before = System.currentTimeMillis();
                statelessKieSession.execute(
                        CommandFactory.newBatchExecution(cmds)
                );

                PerformanceLogger.logTiming(
                        "ONBOARDING_RULE_ENGINE",
                        before,
                        "resulted into rejections %s"
                                .formatted(req.getOnboardingRejectionReasons())
                );

            } else {
                req.getOnboardingRejectionReasons().add(
                        new OnboardingRejectionReason(
                                OnboardingRejectionReason
                                        .OnboardingRejectionReasonType
                                        .TECHNICAL_ERROR,
                                OnboardingConstants
                                        .REJECTION_REASON_RULE_ENGINE_NOT_READY,
                                null,
                                null,
                                null
                        )
                );
            }
        } else {
            log.info(
                    "[ONBOARDING_REQUEST][RULE_ENGINE] Drools not involved for initiative {}",
                    initiative.getInitiativeId()
            );
        }

        log.trace(
                "[ONBOARDING_REQUEST][RULE_ENGINE] Send message prepared: {}",
                req
        );

        return onboarding2EvaluationMapper.apply(
                req,
                initiative,
                req.getOnboardingRejectionReasons()
        );
    }


    private boolean checkIfKieBaseShouldBeInvolved(InitiativeConfig initiative) {
        return !CollectionUtils.isEmpty(initiative.getAutomatedCriteria());
    }

    private boolean checkIfKieBaseContainerIsReady(InitiativeConfig initiative) {
        return onboardingContextHolderService
                .getBeneficiaryRulesKieInitiativeIds()
                .contains(initiative.getInitiativeId());
    }
}