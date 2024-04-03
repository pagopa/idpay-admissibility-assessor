package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.InpsConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = InpsConfiguration.class)
@TestPropertySource(properties = {
        "app.inps.iseeConsultation.base-url=testBaseUrl",
        "app.inps.iseeConsultation.config.connection-timeout=10",
        "app.inps.iseeConsultation.config.request-timeout=20",
        "app.inps.header.userId=usertest",
        "app.inps.secure.cert=testCert",
        "app.inps.secure.key=testKey",
        "app.inps.header.officeCode=testOfficeCode",

})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
 class InpsConfigurationTest {
    @Value("${app.inps.iseeConsultation.base-url}")
    private String baseUrl;
    @Value("${app.inps.iseeConsultation.config.connection-timeout}")
    private Integer connectionTimeout;
    @Value("${app.inps.iseeConsultation.config.request-timeout}")
    private Integer requestTimeout;
    @Value("${app.inps.header.userId}")
    private String userId;
    @Value("${app.inps.header.officeCode}")
    private String officeCode;
    @Value("${app.inps.secure.cert}")
    private String cert;
    @Value("${app.inps.secure.key}")
    private String key;


    @Autowired
    private InpsConfiguration inpsConfiguration;

    @Test
    void givenPropertiesWhenGetInpsThenReturnInfo() {
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();

        Assertions.assertNotNull(inps);
        Assertions.assertEquals(baseUrl, inps.getIseeConsultation().getBaseUrl());
        Assertions.assertEquals(connectionTimeout, inps.getIseeConsultation().getConfig().getConnectionTimeout());
        Assertions.assertEquals(requestTimeout, inps.getIseeConsultation().getConfig().getRequestTimeout());
        Assertions.assertEquals(userId, inps.getHeader().getUserId());
        Assertions.assertEquals(officeCode, inps.getHeader().getOfficeCode());
        Assertions.assertEquals(cert, inps.getSecure().getCert());
        Assertions.assertEquals(key, inps.getSecure().getKey());

    }

    @Test
    void givenNullInpsIseeConsultationWhenGetBaseUrlThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setIseeConsultation(null);

        String baseUrlForInps = inpsConfiguration.getBaseUrlForInps();

        Assertions.assertNull(baseUrlForInps);
    }

    @Test
    void givenNullInpsConfigWhenGetConnectionTimeoutAndRequestTimeoutThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.getIseeConsultation().setConfig(null);

        Integer connectionTimeoutForInps = inpsConfiguration.getConnectionTimeoutForInps();
        Integer requestTimeoutForInps = inpsConfiguration.getRequestTimeoutForInps();

        Assertions.assertNull(connectionTimeoutForInps);
        Assertions.assertNull(requestTimeoutForInps);
    }

    @Test
    void givenNullHeaderWhenGetOfficeCodeAndUserIdThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setHeader(null);

        String officeCodeForInps = inpsConfiguration.getOfficeCodeForInps();
        String userIdForInps = inpsConfiguration.getUserIdForInps();

        Assertions.assertNull(officeCodeForInps);
        Assertions.assertNull(userIdForInps);
    }

    @Test
    void givenNullSecureWhenGetCertAndKeyThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setSecure(null);

        String certForInps = inpsConfiguration.getCertForInps();
        String keyForInps = inpsConfiguration.getKeyForInps();


        Assertions.assertNull(certForInps);
        Assertions.assertNull(keyForInps);
    }

    @Test
    void givenNullInpsWhenGetAllThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setIseeConsultation(null);
        inps.setHeader(null);
        inps.setSecure(null);

        String certForInps = inpsConfiguration.getCertForInps();
        String keyForInps = inpsConfiguration.getKeyForInps();
        String officeCodeForInps = inpsConfiguration.getOfficeCodeForInps();
        String userIdForInps = inpsConfiguration.getUserIdForInps();
        Integer connectionTimeoutForInps = inpsConfiguration.getConnectionTimeoutForInps();
        Integer requestTimeoutForInps = inpsConfiguration.getRequestTimeoutForInps();
        String baseUrlForInps = inpsConfiguration.getBaseUrlForInps();

        Assertions.assertNull(baseUrlForInps);
        Assertions.assertNull(connectionTimeoutForInps);
        Assertions.assertNull(requestTimeoutForInps);
        Assertions.assertNull(officeCodeForInps);
        Assertions.assertNull(userIdForInps);
        Assertions.assertNull(certForInps);
        Assertions.assertNull(keyForInps);

    }



}

