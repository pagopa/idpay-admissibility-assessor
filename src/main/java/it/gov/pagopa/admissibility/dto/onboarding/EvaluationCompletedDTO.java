package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.minidev.json.annotate.JsonIgnore;

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
    @NotEmpty
    private String status;
    @NotNull
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
    private BigDecimal beneficiaryBudget;
    @JsonIgnore
    private Long rankingValue;
}
