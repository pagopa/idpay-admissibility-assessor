package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OnboardingRequestEvaluatorServiceImpl implements OnboardingRequestEvaluatorService {

    private final RuleEngineService ruleEngineService;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public OnboardingRequestEvaluatorServiceImpl(RuleEngineService ruleEngineService, InitiativeCountersRepository initiativeCountersRepository) {
        this.ruleEngineService = ruleEngineService;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public Mono<EvaluationDTO> evaluate(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        final EvaluationDTO result = ruleEngineService.applyRules(onboardingRequest);
        if(OnboardingConstants.ONBOARDING_STATUS_OK.equals(result.getStatus())){
            return initiativeCountersRepository.reserveBudget(onboardingRequest.getInitiativeId(), initiativeConfig.getBeneficiaryInitiativeBudget())
                    .map(c->result)
                    .switchIfEmpty(Mono.defer(()->{
                        result.getOnboardingRejectionReasons().add(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED);
                        result.setStatus(OnboardingConstants.ONBOARDING_STATUS_KO);
                        return Mono.just(result);
                    }));
        }
        return Mono.just(result);
    }
}
