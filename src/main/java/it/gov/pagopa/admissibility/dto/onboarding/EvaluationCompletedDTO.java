package it.gov.pagopa.admissibility.dto.onboarding;

import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.minidev.json.annotate.JsonIgnore;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class EvaluationCompletedDTO extends EvaluationDTO{
    private String initiativeName;
    private String organizationName;
    private LocalDate initiativeEndDate;
    @NotEmpty
    private OnboardingEvaluationStatus status;
    @NotNull
    private List<OnboardingRejectionReason> onboardingRejectionReasons = new ArrayList<>();
    private BigDecimal beneficiaryBudget;
    @JsonIgnore
    private Long rankingValue;
    private String initiativeRewardType;
    private Boolean isLogoPresent;
}
