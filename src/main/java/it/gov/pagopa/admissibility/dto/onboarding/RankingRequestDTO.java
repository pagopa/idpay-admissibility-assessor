package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RankingRequestDTO extends EvaluationDTO {
    private long rankingValue;
    private boolean onboardingKo;

    @Override
    public void setRankingValue(Long rankingValue) {
        this.rankingValue=rankingValue;
    }

    public Long getRankingValue() {
        return rankingValue;
    }
}
