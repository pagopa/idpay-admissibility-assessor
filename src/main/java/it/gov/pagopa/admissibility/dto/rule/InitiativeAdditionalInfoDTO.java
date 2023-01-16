package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class InitiativeAdditionalInfoDTO {
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("argument")
    private String argument;
    @JsonProperty("description")
    private String description;
    @JsonProperty("channels")
    private List<ChannelsDTO> channels;
}
