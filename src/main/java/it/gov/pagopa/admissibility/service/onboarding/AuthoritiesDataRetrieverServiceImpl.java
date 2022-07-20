package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{

    private final OnboardingContextHolderService onboardingContextHolderService;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public boolean retrieve(OnboardingDTO onboardingDTO, InitiativeConfig initiativeConfig) {

        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (use CriteriaCodeService),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error return false and short circuit for the other invocation for the current date
        * if all the calls were successful, return true
        */
        return true;    // TODO

    }
}
