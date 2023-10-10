package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.utils.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AESTokenServiceImpl implements AESTokenService {

    private final String passphrase;
    private final AESUtil aesUtil;

    public AESTokenServiceImpl(@Value("${app.util.crypto.aes.secret-type.pbe.passphrase}")String passphrase, AESUtil aesUtil) {
        this.passphrase = passphrase;
        this.aesUtil = aesUtil;
    }

    @Override
    public String encrypt(String plainText) {
        return aesUtil.encrypt(passphrase, plainText);
    }

    @Override
    public String decrypt(String ciphertext) {
        return aesUtil.decrypt(passphrase, ciphertext);
    }
}
