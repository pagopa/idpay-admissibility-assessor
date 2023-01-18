package it.gov.pagopa.admissibility.rest.agid;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.admissibility.dto.agid.AgidJwtToken;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Slf4j
public abstract class AgidJwtSignature {
    private final AgidJwtToken.AgidJwtTokenHeader tokenHeader;
//    private final AgidJwtToken.AgidJwtTokenPayload tokenPayload; //TODO cancel

    private final String cert;
    private final String key;
    private final String pub;

    protected AgidJwtSignature(AgidJwtToken agidJwtToken,
                            String cert,
                            String key,
                            String pub) {
        this.tokenHeader = agidJwtToken.getHeader();
        this.cert = cert;
        this.key = key;
        this.pub = pub;
    }

    public String createAgidJwt(String digest, AgidJwtTokenPayload agidJwtTokenPayload) {
        log.info("start to create AgidJwt with digest: {}",digest);
        try {
            return JWT.create()
                    .withHeader(createHeaderMap())
                    .withPayload(createClaimMap(digest, agidJwtTokenPayload))
                    .sign(Algorithm.RSA256(getPublicKey(), getPrivateKey()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new IllegalStateException("Something went wrong creating AgidJwt",e);
        }
    }

    private Map<String, Object> createHeaderMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, tokenHeader.getTyp());
        map.put(HeaderParams.ALGORITHM, tokenHeader.getAlg());
        map.put("x5c", List.of(cert));
        log.debug("HeaderMap: {}",map);
        return map;
    }

    private Map<String, Object> createClaimMap(String digest, AgidJwtTokenPayload tokenPayload) { //create utilizzo di Iss, Sub, Aud
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + 5000L;

        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.ISSUER, tokenPayload.getIss());
        map.put(RegisteredClaims.SUBJECT, tokenPayload.getSub());
        map.put(RegisteredClaims.EXPIRES_AT, expireSeconds);
        map.put(RegisteredClaims.AUDIENCE, tokenPayload.getAud());
        map.put(RegisteredClaims.ISSUED_AT, nowSeconds);
        map.put(RegisteredClaims.JWT_ID, UUID.randomUUID().toString());
        map.put("signed_headers", createSignedHeaders(digest));
        log.debug("ClaimMap: {}",map);
        return map;
    }

    private List<Object> createSignedHeaders(String digest) {
        List<Object> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("digest", digest);
        list.add(map);
        Map<String, String> map1 = new HashMap<>();
        map1.put("content-encoding", "UTF-8");
        list.add(map1);
        Map<String, String> map2 = new HashMap<>();
        map2.put("content-type", "application/json");
        list.add(map2);
        return list;
    }

    protected RSAPublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        log.debug("start getPublicKey");
        String pubStringFormat = pemToString(pub);
        try(
            InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pubStringFormat))
        ) {
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
        }
    }


    protected RSAPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        log.debug("start getPrivateKey");
        String keyStringFormat =  pemToString(key);
        try(
            InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat))
        ) {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
        }
    }

    private String pemToString(String target) {
        return target.replaceAll("^-----BEGIN[A-Z|\\s]+-----", "")
                .replaceAll("\\n+", "")
                .replaceAll("-----END[A-Z|\\s]+-----$", "");
    }
}
