package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
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
