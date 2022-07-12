package it.gov.pagopa.dto.onboarding;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingDroolsDTO extends OnboardingDTO{

    private List<String> onboardingRejectionReasons = new ArrayList<>();
}
