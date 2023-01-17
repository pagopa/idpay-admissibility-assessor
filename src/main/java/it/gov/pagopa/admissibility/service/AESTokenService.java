package it.gov.pagopa.admissibility.service;

public interface AESTokenService {

    String encrypt(String plainText);
    String decrypt(String plainText);

}
