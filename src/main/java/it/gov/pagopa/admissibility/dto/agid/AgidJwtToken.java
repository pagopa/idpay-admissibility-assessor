package it.gov.pagopa.admissibility.dto.agid;

import lombok.Data;

@Data
public class AgidJwtToken {
    protected AgidJwtTokenHeader header;
    protected AgidJwtTokenPayload payload;

    @Data
    public static class AgidJwtTokenHeader {
        private String alg;
        private String kid;
        private String typ;
    }

    @Data
    public static class AgidJwtTokenPayload {
        private String iss;
        private String sub;
        private String aud;
    }
}