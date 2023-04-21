package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Document("onboarding_families")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingFamilies {
    @Id
    private String id;
    private OnboardingFamilyEvaluationStatus status;
    private Set<String> memberIds;
}
