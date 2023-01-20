package it.gov.pagopa.admissibility.dto.agid;

import lombok.Data;

@Data
public class AgidJwtToken {
    protected AgidJwtTokenHeader header;

    @Data
    public static class AgidJwtTokenHeader {
        private String alg;
        private String typ;
    }
}