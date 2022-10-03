package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.service.onboarding.check.OnboardingCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class OnboardingCheckServiceImpl implements OnboardingCheckService {

    private final List<OnboardingCheck> checks;

    public OnboardingCheckServiceImpl(List<OnboardingCheck> checks) {
        this.checks = checks;
    }


    @Override
    public OnboardingRejectionReason check(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        log.trace("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] evaluating onboarding checks of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
        return checks.stream().map(f -> f.apply(onboardingRequest, onboardingContext)).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
