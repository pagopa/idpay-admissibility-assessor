package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.utils.AESUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.util.crypto.aes.secret-type.pbe.passphrase=PASSPHRASE"
        })
@ContextConfiguration(classes = {AESTokenServiceImpl.class})
@ExtendWith(SpringExtension.class)
class AESTokenServiceTest {

    private static final String PLAINTEXT = "PLAINTEXT";
    private static final String CIPHERTEXT = "CIPHERTEXT";
    private static final String PASSPHRASE = "PASSPHRASE";
    private static final String PRIMARY_TOKEN_IO = "PRIMARY_TOKEN_IO";

    @Autowired
    private AESTokenServiceImpl aesTokenServiceImpl;

    @MockBean
    AESUtil aesUtil;

    @Test
    void givenPlainTextAndPassphrase_whenEncrypt_thenSuccess(){
        when(aesUtil.encrypt(PASSPHRASE, PLAINTEXT)).thenReturn(PRIMARY_TOKEN_IO);
        String encryptedToken = aesTokenServiceImpl.encrypt(PLAINTEXT);
        assertEquals(PRIMARY_TOKEN_IO, encryptedToken);
    }

    @Test
    void givenCipherTextAndPassphrase_whenDecrypt_thenSuccess(){
        when(aesUtil.decrypt(PASSPHRASE, CIPHERTEXT)).thenReturn(PLAINTEXT);
        String decryptedToken = aesTokenServiceImpl.decrypt(CIPHERTEXT);
        assertEquals(PLAINTEXT, decryptedToken);
    }

}
