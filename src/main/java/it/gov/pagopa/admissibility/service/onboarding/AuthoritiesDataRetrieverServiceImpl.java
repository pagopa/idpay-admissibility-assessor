package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{

    private final OnboardingContextHolderService onboardingContextHolderService;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (use CriteriaCodeService),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error postpone the message and short circuit for the other invocation for the current date
        * if all the calls were successful return a Mono with the request
        */
        return Mono.just(onboardingRequest);    // TODO

    }
}
