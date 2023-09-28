package it.gov.pagopa.common.reactive.pdnd.dto;

import com.auth0.jwt.algorithms.Algorithm;
import lombok.Data;

@Data
public class PdndAuthData {
    private String agidJwtTrackingEvidence;
    private String clientAssertion;
    private String accessToken;
    private Algorithm jwtSignAlgorithm;
}
