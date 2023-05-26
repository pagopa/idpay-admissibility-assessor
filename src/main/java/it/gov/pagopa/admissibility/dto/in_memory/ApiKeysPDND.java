package it.gov.pagopa.admissibility.dto.in_memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.config.JsonConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Base64;
@Data
@AllArgsConstructor
@Builder
public class ApiKeysPDND {
    private final String apiKeyClientId;
    private final String apiKeyClientAssertion;
    private final AgidJwtTokenPayload agidJwtTokenPayload;


    public ApiKeysPDND(String apiKeyClientId, String apiKeyClientAssertion) throws JsonProcessingException {
        this.apiKeyClientId = apiKeyClientId;
        this.apiKeyClientAssertion = apiKeyClientAssertion;

        String agidJwtTokenPayloadString = retrieveAgidTokenPayload(apiKeyClientAssertion);
        agidJwtTokenPayload = new JsonConfig().objectMapper()
                .readValue(agidJwtTokenPayloadString,AgidJwtTokenPayload.class);
    }

    private String retrieveAgidTokenPayload(String clientAssertion) {
        String[] splitClientAssertion = clientAssertion.split("\\.");
        return new String(Base64.getDecoder().decode(splitClientAssertion[1]));
    }
}
