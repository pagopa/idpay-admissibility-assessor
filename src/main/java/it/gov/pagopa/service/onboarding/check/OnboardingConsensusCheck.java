package it.gov.pagopa.service.onboarding.check;

import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(0)
public class OnboardingConsensusCheck implements OnboardingCheck {

    @Override
    public String apply(OnboardingDTO onboardingDTO) {
        return null;    // TODO
    }
}
