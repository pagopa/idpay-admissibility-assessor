package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class InitiativeConfig {

    private String initiativeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pdndToken;
    private List<String> automatedCriteriaCodes;
    private BigDecimal initiativeBudget;
    private BigDecimal beneficiaryInitiativeBudget;

    @ReadOnlyProperty @Builder.Default
    private Long onboarded=0L;
    @ReadOnlyProperty @Builder.Default
    private BigDecimal reservedInitiativeBudget=BigDecimal.ZERO;
}
