package it.gov.pagopa.admissibility.dto.onboarding;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRejectionReason implements Serializable {
    @NotNull
    private OnboardingRejectionReasonType type;
    @NotNull
    private String code;
    private String authority;
    private String authorityLabel;
    private String detail;

    public enum OnboardingRejectionReasonType {
        TECHNICAL_ERROR,
        CONSENSUS_MISSED,
        INVALID_REQUEST,
        BUDGET_EXHAUSTED,
        AUTOMATED_CRITERIA_FAIL,
        ISEE_TYPE_KO,
        FAMILY_KO,
        RESIDENCE_KO,
        BIRTHDATE_KO,
        OUT_OF_RANKING
    }
}
