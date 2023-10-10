package it.gov.pagopa.admissibility.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class AESUtil {
    public final String pbeAlgorithm;
    public final String encoding;
    private final String salt;
    private final String iv;
    private final int keySize;
    private final int iterationCount;
    private final int gcmTagLength;
    private final String cipherInstance;

    public AESUtil(@Value("${app.util.crypto.aes.cipherInstance}") String cipherInstance,
                   @Value("${app.util.crypto.aes.encoding}") String encoding,
                   @Value("${app.util.crypto.aes.secret-type.pbe.algorithm}") String pbeAlgorithm,
                   @Value("${app.util.crypto.aes.secret-type.pbe.salt}") String salt,
                   @Value("${app.util.crypto.aes.secret-type.pbe.keySize}") int keySize,
                   @Value("${app.util.crypto.aes.secret-type.pbe.iterationCount}") int iterationCount,
                   @Value("${app.util.crypto.aes.mode.gcm.iv}") String iv,
                   @Value("${app.util.crypto.aes.mode.gcm.tLen}") int gcmTagLength) {
        this.cipherInstance = cipherInstance;
        this.encoding = encoding;
        this.pbeAlgorithm = pbeAlgorithm;
        this.salt = salt;
        this.keySize = keySize;
        this.iterationCount = iterationCount;
        this.iv = iv;
        this.gcmTagLength = gcmTagLength;
    }

    private SecretKey generateKey(String passphrase) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(pbeAlgorithm);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt.getBytes(), iterationCount, keySize);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw fail(e);
        }
    }

    public String encrypt(String passphrase, String plaintext) {
        try {
            SecretKey key = generateKey(passphrase);
            byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, plaintext.getBytes(encoding));
            return base64(encrypted);
        }
        catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    public String decrypt(String passphrase, String ciphertext) {
        try {
            SecretKey key = generateKey(passphrase);
            byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, base64(ciphertext));
            return new String(decrypted, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    private byte[] doFinal(int encryptMode, SecretKey key, byte[] bytes) {
        try {
            GCMParameterSpec parameterSpec = new GCMParameterSpec(gcmTagLength * 8, iv.getBytes());
            Cipher cipher = Cipher.getInstance(this.cipherInstance);
            cipher.init(encryptMode, key, parameterSpec);
            return cipher.doFinal(bytes);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException
                 | InvalidAlgorithmParameterException
                 | IllegalBlockSizeException
                 | BadPaddingException e) {
            throw fail(e);
        }
    }

    public static String base64(byte[] bytes) {
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] base64(String str) {
        return Base64.getDecoder().decode(str);
    }

    private IllegalStateException fail(Exception e) {
        return new IllegalStateException(e);
    }

}
