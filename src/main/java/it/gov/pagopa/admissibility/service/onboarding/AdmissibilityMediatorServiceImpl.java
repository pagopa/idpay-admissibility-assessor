package it.gov.pagopa.admissibility.service.onboarding;


import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.admissibility.utils.Constants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
public class AdmissibilityMediatorServiceImpl implements AdmissibilityMediatorService {

    private final OnboardingCheckService onboardingCheckService;
    private final AuthoritiesDataRetrieverService authoritiesDataRetrieverService;
    private final RuleEngineService ruleEngineService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    public AdmissibilityMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, RuleEngineService ruleEngineService, Onboarding2EvaluationMapper onboarding2EvaluationMapper) {
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.ruleEngineService = ruleEngineService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
    }


    /**
     * This component will take a {@link OnboardingDTO} and will calculate the {@link EvaluationDTO}
     */
    @Override
    public Flux<EvaluationDTO> execute(Flux<OnboardingDTO> onboardingDTOFlux) {

        return onboardingDTOFlux.map(o -> {
            Map<String,Object> onboardingContext = new HashMap<>();
            String rejectionReason = onboardingCheckService.check(o, onboardingContext);
            if (StringUtils.hasText(rejectionReason)) {
                log.info("[ONBOARDING_KO] Onboarding request failed: {}",rejectionReason);
                return onboarding2EvaluationMapper.apply(o, Collections.singletonList(rejectionReason));
            } else {
                if(authoritiesDataRetrieverService.retrieve(o,(InitiativeConfig) onboardingContext.get(ONBOARDING_CONTEXT_INITIATIVE_KEY))) {
                    return ruleEngineService.applyRules(o);
                }else {
                    log.info("[ONBOARDING_POSTPONED] Onboarding request postponed");
                    //TODO reschedule to next day
                    return null;
                }
            }
        });
    }
}
