package it.gov.pagopa.admissibility.rest.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.admissibility.config.AnprConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

@Component
@Slf4j
public class AgidJwtSignature {
    private final AnprConfig.TokenHeader tokenHeader;
    private final AnprConfig.TokenPayload tokenPayload;

    private final String cert;
    private final String key;
    private final String pub;

    public AgidJwtSignature(AnprConfig.TokenHeader tokenHeader,
                            AnprConfig.TokenPayload tokenPayload,
                            @Value("${app.anpr.web-client.secure.cert}") String cert,
                            @Value("${app.anpr.web-client.secure.key}") String key,
                            @Value("${app.anpr.web-client.secure.pub}") String pub) {
        this.tokenHeader = tokenHeader;
        this.tokenPayload = tokenPayload;
        this.cert = cert;
        this.key = key;
        this.pub = pub;
    }

    public String createAgidJwt(String digest) {
        log.info("start to createAgidJwt with digest: {}",digest);
        try {
            return JWT.create()
                    .withHeader(createHeaderMap(tokenHeader))
                    .withPayload(createClaimMap(digest,tokenPayload))
                    .sign(Algorithm.RSA256(getPublicKey(), getPrivateKey()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new IllegalStateException("Something went wrong creating AgidJwt",e);
        }
    }

    private Map<String, Object> createHeaderMap(AnprConfig.TokenHeader th) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, th.getTyp());
        map.put(HeaderParams.ALGORITHM, th.getAlg());
        map.put("x5c", List.of(cert));
        log.debug("HeaderMap: {}",map);
        return map;
    }

    private Map<String, Object> createClaimMap(String digest, AnprConfig.TokenPayload tp) {
        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.ISSUER, tp.getIss());
        map.put(RegisteredClaims.SUBJECT, tp.getSub());
        map.put(RegisteredClaims.EXPIRES_AT, tp.getExp());
        map.put(RegisteredClaims.AUDIENCE, tp.getAud());
        map.put(RegisteredClaims.ISSUED_AT, tp.getIat());
        map.put(RegisteredClaims.JWT_ID, tp.getJti());
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
        String pubStringFormat =  pub.replace("-----BEGIN PUBLIC KEY-----", "")// TODO extract into utils
                .replaceAll("\\n", "")
                .replaceAll("\\r", "")
                .replace("-----END PUBLIC KEY-----", "");

        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pubStringFormat)); //TODO extract into utils
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
    }

    protected RSAPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        log.debug("start getPrivateKey");
        String keyStringFormat =  key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\r", "")
                .replace("-----END PRIVATE KEY-----", "");

        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat));
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
    }

}
