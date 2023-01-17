package it.gov.pagopa.admissibility.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AESUtilTest {

    private AESUtil util;

    public static final String CIPHER_INSTANCE = "AES/GCM/NoPadding";
    public static final String ENCODING = "UTF-8";
    public static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final String SALT = "SALT_SAMPLE";
    public static final int KEY_SIZE = 256;
    public static final int ITERATION_COUNT = 10000;
    public static final String PLAIN_TEXT = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy ";
    public static final String PASSPHRASE = "the quick brown fox jumps over the lazy dog";
    public static final String GCM_IV = "IV_SAMPLE";
    public static final int GCM_TAG_LENGTH = 16;

    public static final String CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000 = "wzQD/tLo8vghu5EvhZYDaUnp1B5x1e1pBZJHTLkUlNYLGh/rrzkGgFMtrjZUEgOsZoKfJ5gL4DWQN209PooN0kwq6XTt1Kuj5NF2tvyO5yadXmqvV0DbRIQ";

    @Test
    void testMultiEncrypt() {
        util = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        String encrypt = util.encrypt(PASSPHRASE, PLAIN_TEXT);
        assertEquals(CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000, encrypt);
        encrypt = util.encrypt(PASSPHRASE, PLAIN_TEXT);
        assertEquals(CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000, encrypt);
    }

    @Test
    void testMultiDecrypt() {
        util = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        String decrypt = util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        assertEquals(PLAIN_TEXT, decrypt);
        decrypt = util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        assertEquals(PLAIN_TEXT, decrypt);
    }


    //EXCEPTION TESTS:

    @Test
    void testGenerateKey_throwInvalidKeyException() {
        util = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, 89, 18, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.encrypt(PASSPHRASE, PLAIN_TEXT);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void testGenerateKey_throwNoSuchAlgorithmException(){
        util = new AESUtil(CIPHER_INSTANCE, ENCODING, "ALGORITHM_INVALID", SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.encrypt(PASSPHRASE, PLAIN_TEXT);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }


    @Test
    void testEncrypt_throwUnsupportedEncodingException(){
        util = new AESUtil(CIPHER_INSTANCE, "ENCODING_NOT_VALID", PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.encrypt(PASSPHRASE, PLAIN_TEXT);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void testDecrypt_throwUnsupportedEncodingException(){
        util = new AESUtil(CIPHER_INSTANCE, "ENCODING_NOT_VALID", PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void AESUtil_throwNoSuchAlgorithmException() {
        util = new AESUtil("INSTANCE_NOT_VALID", ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void AESUtil_throwNoSuchPaddingException() {
        util = new AESUtil("AES/GCM", ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, GCM_IV, GCM_TAG_LENGTH);
        Executable executable = () -> util.decrypt(PASSPHRASE, CIPHER_TEXT_AES_GCM_NO_PADDING_KEY_256_ITERATION_10000);
        Assertions.assertThrows(IllegalStateException.class, executable);
    }
}
