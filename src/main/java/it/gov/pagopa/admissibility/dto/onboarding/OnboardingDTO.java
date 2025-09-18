package it.gov.pagopa.admissibility.dto.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime tcAcceptTimestamp;
    private LocalDateTime criteriaConsensusTimestamp;

    // Authorities data fetched if the initiative requires them
    private BigDecimal isee;
    private Residence residence;
    private BirthDate birthDate;
    private Family family;

    // Info filled during processing
    private boolean budgetReserved;

    //
    private String serviceId;

    private Boolean verifyIsee;
    private String userMail;
    private String channel;
    // data fetched if the initiative requires them
    private Boolean underThreshold;
}
