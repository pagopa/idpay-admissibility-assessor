package it.gov.pagopa.admissibility.service.onboarding.notifier;

import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;

public interface RankingNotifierService {
    boolean notify(RankingRequestDTO rankingRequestDTO);
}
