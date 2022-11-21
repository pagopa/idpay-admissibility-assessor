package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RankingRequestDTO extends EvaluationDTO {
    private BigDecimal rankingValue;
}
