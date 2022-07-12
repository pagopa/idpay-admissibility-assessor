package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.service.onboarding.check.OnboardingCheck;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class OnboardingCheckServiceImpl implements OnboardingCheckService {

    private final List<OnboardingCheck> checks;

    public OnboardingCheckServiceImpl(List<OnboardingCheck> checks) {
        this.checks = checks;
    }


    @Override
    public String check(OnboardingDTO onboardingDTO) {
        return checks.stream().map(f -> f.apply(onboardingDTO)).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
