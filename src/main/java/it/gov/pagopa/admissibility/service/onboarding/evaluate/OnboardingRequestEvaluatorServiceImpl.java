package it.gov.pagopa.admissibility.service.onboarding.evaluate;

import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OnboardingRequestEvaluatorServiceImpl implements OnboardingRequestEvaluatorService {

    private final RuleEngineService ruleEngineService;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public OnboardingRequestEvaluatorServiceImpl(RuleEngineService ruleEngineService, InitiativeCountersRepository initiativeCountersRepository) {
        this.ruleEngineService = ruleEngineService;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        final EvaluationDTO result = ruleEngineService.applyRules(onboardingRequest, initiativeConfig);
        if (result instanceof EvaluationCompletedDTO evaluationCompletedDTO) {
            if (OnboardingEvaluationStatus.ONBOARDING_OK.equals((evaluationCompletedDTO.getStatus()))) {
                log.trace("[ONBOARDING_REQUEST] [RULE_ENGINE] rule engine meet automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                calculateBeneficiaryBudget(onboardingRequest, initiativeConfig, evaluationCompletedDTO);
                long deallocatedBudget = Boolean.TRUE.equals(onboardingRequest.getVerifyIsee()) ?
                        initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents() - evaluationCompletedDTO.getBeneficiaryBudgetCents() : 0;

                Mono<EvaluationDTO> budgetMono = (deallocatedBudget > 0)
                        ? initiativeCountersRepository.deallocatedPartialBudget(evaluationCompletedDTO.getInitiativeId(), deallocatedBudget)
                        .thenReturn(evaluationCompletedDTO)
                        : Mono.just(evaluationCompletedDTO);

                return budgetMono
                        .map(c -> {
                            log.info("[ONBOARDING_REQUEST] [ONBOARDING_OK] [BUDGET_RESERVATION] user {} reserved budget on initiative {}", onboardingRequest.getUserId(), initiativeConfig.getInitiativeId());
                            onboardingRequest.setBudgetReserved(true);

                            return evaluationCompletedDTO;
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [BUDGET_RESERVATION] initiative {} exhausted", initiativeConfig.getInitiativeId());

                            evaluationCompletedDTO.getOnboardingRejectionReasons().add(OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                                    .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                                    .build());
                            evaluationCompletedDTO.setStatus(OnboardingEvaluationStatus.ONBOARDING_KO);
                            return Mono.just(evaluationCompletedDTO);
                        }))
                        .map(EvaluationDTO.class::cast);
            } else {
                log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [RULE_ENGINE] Onboarding request of user {} into initiative {} failed: {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId(), evaluationCompletedDTO.getOnboardingRejectionReasons());
            }
        }
        return Mono.just(result);
    }

    private void calculateBeneficiaryBudget(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, EvaluationCompletedDTO result) {
        if(initiativeConfig.getIseeThresholdCode() != null && initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents() != null
            && Boolean.TRUE.equals(onboardingRequest.getVerifyIsee()) && Boolean.TRUE.equals(onboardingRequest.getUnderThreshold())){
                result.setBeneficiaryBudgetCents(initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents());
        } else {
            result.setBeneficiaryBudgetCents(initiativeConfig.getBeneficiaryInitiativeBudgetCents());
        }
    }
}
