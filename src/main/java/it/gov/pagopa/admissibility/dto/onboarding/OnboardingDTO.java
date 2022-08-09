package it.gov.pagopa.admissibility.dto.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.extra.DataNascita;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residenza;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDTO {
    private String userId;
    private String initiativeId;
    private boolean tc;
    private String status;
    private Boolean pdndAccept;
    private Map<String, Boolean> selfDeclarationList;
    private LocalDateTime tcAcceptTimestamp;
    private LocalDateTime criteriaConsensusTimestamp;

    // Authorities data fetched if the initiative requires them
    private BigDecimal isee;
    private Residenza residenza;
    private DataNascita birthDate;
}
