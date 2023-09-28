package it.gov.pagopa.admissibility.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {
    private String entityId;
    private String operationType;
    private LocalDateTime operationTime;
}
