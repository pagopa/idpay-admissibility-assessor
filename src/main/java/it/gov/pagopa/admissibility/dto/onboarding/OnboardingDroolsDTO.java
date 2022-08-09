package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OnboardingDroolsDTO extends OnboardingDTO{
    private List<OnboardingRejectionReason> onboardingRejectionReasons = new ArrayList<>();
}
