package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingRequestDTO {
    @NotEmpty
    private String userId;
    @NotEmpty
    private String initiativeId;
    private String initiativeName;
    private LocalDate initiativeEndDate;
    private String organizationId;
    @NotNull
    private LocalDateTime admissibilityCheckDate;
    @NotNull
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
    private BigDecimal beneficiaryBudget;
    private String serviceId;
    private LocalDateTime criteriaConsensusTimestamp;
    private BigDecimal rankingValue;
}
