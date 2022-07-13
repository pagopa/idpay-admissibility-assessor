package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.rest.initiative.InitiativeRestService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(0)
public class OnboardingConsensusCheck implements OnboardingCheck {

    private final InitiativeRestService initiativeRestService;

    public OnboardingConsensusCheck(InitiativeRestService initiativeRestService) {
        this.initiativeRestService = initiativeRestService;
    }

    @Override
    public String apply(OnboardingDTO onboardingDTO) {

        return null;    // TODO
    }
}
