package it.gov.pagopa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}
