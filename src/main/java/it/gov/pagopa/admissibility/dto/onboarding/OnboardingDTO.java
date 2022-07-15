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

    String userId;
    String initiativeId;
    boolean tc;
    String status;
    Boolean pdndAccept;
    Map<String, Boolean> selfDeclarationList;
    LocalDateTime tcAcceptTimestamp;
    LocalDateTime criteriaConsensusTimestamp;

    // Authorities data fetched if the initiative requires them
    BigDecimal isee;
    Residenza residenza;
    DataNascita birthDate;
}
