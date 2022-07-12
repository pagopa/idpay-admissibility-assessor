package it.gov.pagopa.dto.onboarding;

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

    String userId;
    String initiativeId;
    String status;
    LocalDateTime admissibilityCheckDate;
    List<String> onboardingRejectionReasons;
}
