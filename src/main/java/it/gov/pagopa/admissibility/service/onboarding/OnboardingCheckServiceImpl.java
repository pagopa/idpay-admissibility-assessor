package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.service.onboarding.check.OnboardingCheck;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
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
