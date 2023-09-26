package it.gov.pagopa.common.crypto.utils;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

public class JwtUtils {
    private JwtUtils() {
    }

    /** {@link Algorithm} which will use local provided pemRsaPublicKey and pemRsaPrivateKey */
    public static Algorithm buildLocalRsaKeysJwtSignAlgorithm(String pemRsaPublicKey, String pemRsaPrivateKey) {
        try {
            return Algorithm.RSA256(CertUtils.pemPub2PublicKey(pemRsaPublicKey), CertUtils.pemKey2PrivateKey(pemRsaPrivateKey));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new IllegalStateException("Cannot create local keys based Jwt Sign Algorithm!", e);
        }
    }

    /** It will create a JWT using standard header */
    public static String createJwt(String kid, Map<String, Object> claims, Algorithm jwtSignAlgorithm) {
        return JWT.create()
                .withHeader(createHeaderMap(kid))
                .withPayload(claims)
                .sign(jwtSignAlgorithm);
    }

    private static Map<String, Object> createHeaderMap(String kid) {
        return Map.of(
                HeaderParams.TYPE, JOSEObjectType.JWT,
                HeaderParams.ALGORITHM, JWSAlgorithm.ES256,
                HeaderParams.KEY_ID, kid
        );
    }
}
