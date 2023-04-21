package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

@Document("onboarding_families")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingFamilies {
    @Id
    private String id;
    private Set<String> memberIds;
    private OnboardingFamilyEvaluationStatus status;
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
}
