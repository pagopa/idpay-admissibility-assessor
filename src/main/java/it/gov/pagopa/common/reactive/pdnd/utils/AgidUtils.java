package it.gov.pagopa.common.reactive.pdnd.utils;

import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.nimbusds.jwt.JWTClaimNames;
import it.gov.pagopa.common.reactive.pdnd.config.PdndConfig;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndAuthData;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.crypto.utils.CryptoUtils;
import it.gov.pagopa.common.crypto.utils.JwtUtils;
import jakarta.xml.bind.DatatypeConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AgidUtils {

    private static final String CONTENT_ENCODING_LOWERCASE = HttpHeaders.CONTENT_ENCODING.toLowerCase();
    private static final String CONTENT_TYPE_LOWERCASE = HttpHeaders.CONTENT_TYPE.toLowerCase();

    // Algoritm logic could contain blocking code, using a Mono which allows blocking logic
    private static final Mono<String> BLOCK_ALLOWED_MONO = Mono.just("x").publishOn(Schedulers.boundedElastic());
    private static final Random random = new Random();

    private AgidUtils() {
    }

    /**
     * Algorithm could contain blocking logic, thus we are working with Mono
     */
    public static Mono<PdndAuthData> preparePdndAuthData2invokePdnd(PdndServiceConfig<?> pdndServiceConfig, PdndConfig pdndConfig, PdndInitiativeConfig pdndInitiativeConfig, Algorithm jwtSignAlgorithm) {
        PdndAuthData out = new PdndAuthData();
        out.setJwtSignAlgorithm(jwtSignAlgorithm);

        return buildAgidJwtTrackingEvidence(pdndServiceConfig, pdndInitiativeConfig, jwtSignAlgorithm)
                .flatMap(agidJwtTrackingEvidence -> {
                    out.setAgidJwtTrackingEvidence(agidJwtTrackingEvidence);
                    return buildPdndClientAssertion(pdndConfig, pdndServiceConfig.getAuthExpirationSeconds(), pdndInitiativeConfig, agidJwtTrackingEvidence, jwtSignAlgorithm);
                })
                .map(clientAssertion -> {
                    out.setClientAssertion(clientAssertion);
                    return out;
                });

    }

    /** To build AGID TrackingEvidence */
    public static Mono<String> buildAgidJwtTrackingEvidence(PdndServiceConfig<?> pdndServiceConfig, PdndInitiativeConfig pdndInitiativeConfig, Algorithm jwtSignAlgorithm) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + pdndServiceConfig.getAuthExpirationSeconds();

        return BLOCK_ALLOWED_MONO
                .map(x -> JwtUtils.createJwt(pdndInitiativeConfig.getKid(), Map.of(
                                JWTClaimNames.AUDIENCE, pdndServiceConfig.getAudience(),
                                JWTClaimNames.ISSUER, pdndInitiativeConfig.getClientId(),
                                "purposeId", pdndInitiativeConfig.getPurposeId(),
                                JWTClaimNames.ISSUED_AT, nowSeconds,
                                JWTClaimNames.EXPIRATION_TIME, expireSeconds,
                                "dnonce", "%013d".formatted(random.nextLong(9999999999999L)),
                                "userLocation", pdndServiceConfig.getAgidConfig().getEnv(),
                                "userID", pdndServiceConfig.getAgidConfig().getUserId(),
                                "LoA", "LOA3",
                                JWTClaimNames.JWT_ID, UUID.randomUUID().toString()
                        ),
                        jwtSignAlgorithm));
    }

    /** To build PDND client_assertion */
    public static Mono<String> buildPdndClientAssertion(PdndConfig pdndConfig, long pdndJwtExpirationSeconds, PdndInitiativeConfig pdndInitiativeConfig, String agidJwtTrackingEvidence, Algorithm jwtSignAlgorithm) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + pdndJwtExpirationSeconds;

        return BLOCK_ALLOWED_MONO
                .map(x -> JwtUtils.createJwt(pdndInitiativeConfig.getKid(), Map.of(
                                RegisteredClaims.ISSUER, pdndInitiativeConfig.getClientId(),
                                RegisteredClaims.SUBJECT, pdndInitiativeConfig.getClientId(),
                                RegisteredClaims.AUDIENCE, pdndConfig.getAudience(),
                                "purposeId", pdndInitiativeConfig.getPurposeId(),
                                RegisteredClaims.JWT_ID, UUID.randomUUID().toString(),
                                RegisteredClaims.ISSUED_AT, nowSeconds,
                                RegisteredClaims.EXPIRES_AT, expireSeconds,
                                "digest", Map.of(
                                        "alg", "SHA256",
                                        "value", DatatypeConverter.printHexBinary(CryptoUtils.sha256(agidJwtTrackingEvidence))
                                )
                        ),
                        jwtSignAlgorithm));
    }

    public static Mono<String> buildAgidJwtSignature(PdndServiceConfig<?> pdndServiceConfig, PdndInitiativeConfig pdndInitiativeConfig, Algorithm jwtSignAlgorithm, String digest) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + pdndServiceConfig.getAuthExpirationSeconds();

        return BLOCK_ALLOWED_MONO
                .map(x -> JwtUtils.createJwt(pdndInitiativeConfig.getKid(), Map.of(
                                RegisteredClaims.AUDIENCE, pdndServiceConfig.getAudience(),
                                RegisteredClaims.ISSUER, pdndInitiativeConfig.getClientId(),
                                RegisteredClaims.SUBJECT, pdndInitiativeConfig.getClientId(),
                                RegisteredClaims.ISSUED_AT, nowSeconds,
                                RegisteredClaims.EXPIRES_AT, expireSeconds,
                                RegisteredClaims.JWT_ID, UUID.randomUUID().toString(),
                        "signed_headers", Map.of(
                                        "digest", digest,
                                        CONTENT_ENCODING_LOWERCASE, StandardCharsets.UTF_8.name(),
                                        CONTENT_TYPE_LOWERCASE, MediaType.APPLICATION_JSON_VALUE
                                )
                        ),
                        jwtSignAlgorithm));
    }

    public static String buildDigest(String value) {
        return "SHA-256" + CryptoUtils.sha256Base64(value);
    }
}
