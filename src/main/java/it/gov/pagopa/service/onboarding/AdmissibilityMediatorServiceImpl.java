package it.gov.pagopa.service.onboarding;


import it.gov.pagopa.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;

@Service
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
            String rejectionReason = onboardingCheckService.check(o);
            if (StringUtils.hasText(rejectionReason)) {
                return onboarding2EvaluationMapper.apply(o, Collections.singletonList(rejectionReason));
            } else {
                authoritiesDataRetrieverService.retrieve(o);
                return ruleEngineService.applyRules(o);
            }
        });
    }
}
