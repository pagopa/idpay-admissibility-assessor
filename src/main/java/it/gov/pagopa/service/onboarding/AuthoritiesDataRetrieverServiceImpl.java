package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.InitiativeConfig;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{

    private final OnboardingContextHolderService onboardingContextHolderService;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public boolean retrieve(OnboardingDTO onboardingDTO) {
        InitiativeConfig initiativeConfig = onboardingContextHolderService.getInitiativeConfig(onboardingDTO.getInitiativeId());
        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (TBD),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error return false
        * if all the calls were successful, return true
        */
        return true;    // TODO

    }
}
