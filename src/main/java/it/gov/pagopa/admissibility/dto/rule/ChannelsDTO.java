package it.gov.pagopa.admissibility.dto.rule;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class ChannelsDTO {
    private String type;
    private String contact;
}
