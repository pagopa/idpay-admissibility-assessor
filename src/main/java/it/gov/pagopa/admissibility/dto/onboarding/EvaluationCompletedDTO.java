package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class EvaluationCompletedDTO extends EvaluationDTO{
    private String initiativeName;
    private LocalDate initiativeEndDate;
    private String organizationId;
    @NotEmpty
    private String status;
    @NotNull
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
    private BigDecimal beneficiaryBudget;
}
