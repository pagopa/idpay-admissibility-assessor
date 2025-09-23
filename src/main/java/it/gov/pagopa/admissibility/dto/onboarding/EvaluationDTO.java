package it.gov.pagopa.admissibility.dto.onboarding;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class EvaluationDTO {
    @NotEmpty
    private String userId;
    private String familyId;
    @NotEmpty
    private String initiativeId;
    @NotEmpty
    private String organizationId;
    @NotNull
    private LocalDateTime admissibilityCheckDate;
    private LocalDateTime criteriaConsensusTimestamp;

    public abstract Long getRankingValue();
    public abstract void setRankingValue(Long rankingValue);
    private Set<String> memberIds;

    //
    private String serviceId;

    private Boolean verifyIsee;
    private String userMail;
    private String channel;
}
