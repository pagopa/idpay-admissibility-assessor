package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTO {
    private String userId;
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String status;
    private LocalDateTime admissibilityCheckDate;
    private List<String> onboardingRejectionReasons;
}
