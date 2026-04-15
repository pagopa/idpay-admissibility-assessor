package it.gov.pagopa.admissibility.dto.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDroolsDTO {

    private List<OnboardingRejectionReason> onboardingRejectionReasons = new ArrayList<>();
    private String userId;
    private String initiativeId;
    private boolean tc;
    private String status;
    private Boolean pdndAccept;
    private OffsetDateTime tcAcceptTimestamp;
    private OffsetDateTime criteriaConsensusTimestamp;

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
    private String name;
    private String surname;
    // data fetched if the initiative requires them underThreshold or deformed
    private Boolean underThreshold;
}
