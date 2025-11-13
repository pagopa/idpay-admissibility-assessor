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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = InpsConfiguration.class)
@TestPropertySource(properties = {
        "app.inps-mock.enabled-isee=true",
        "app.inps-mock.base-url=testMockBaseUrl",
        "app.inps.iseeConsultation.base-url=testRealBaseUrl",
        "app.inps.iseeConsultation.config.connection-timeout=10",
        "app.inps.iseeConsultation.config.request-timeout=20",
        "app.inps.header.userId=usertest",
        "app.inps.header.officeCode=testOfficeCode",
        "app.inps.secure.cert=testCert",
        "app.inps.secure.key=testKey"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InpsConfigurationTest {

    @Value("${app.inps.secure.cert}")
    private String cert;

    @Autowired
    private InpsConfiguration inpsConfiguration;

    @Test
    void givenPropertiesWhenGetInpsThenReturnInfo() {
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        Assertions.assertNotNull(inps);

        InpsConfiguration.InpsMock mock = inpsConfiguration.getInpsMock();
        Assertions.assertNotNull(mock);

        String expectedBaseUrl = mock.isEnabledIsee()
                ? mock.getBaseUrl()
                : inps.getIseeConsultation().getBaseUrl();

        byte[] decodedBytes = Base64.getDecoder().decode(cert);
        String expectedCert = new String(decodedBytes, StandardCharsets.UTF_8);

        Assertions.assertEquals(expectedBaseUrl, inpsConfiguration.getBaseUrlForInps());
        Assertions.assertEquals(10, inps.getIseeConsultation().getConfig().getConnectionTimeout());
        Assertions.assertEquals(20, inps.getIseeConsultation().getConfig().getRequestTimeout());
        Assertions.assertEquals("usertest", inps.getHeader().getUserId());
        Assertions.assertEquals("testOfficeCode", inps.getHeader().getOfficeCode());
        Assertions.assertEquals(expectedCert, inps.getSecure().getCert());
        Assertions.assertEquals("testKey", inps.getSecure().getKey());
    }

    @Test
    void givenMockEnabledTrueThenBaseUrlIsMock() {
        inpsConfiguration.getInpsMock().setEnabledIsee(true);
        inpsConfiguration.getInpsMock().setBaseUrl("mockBaseUrl");
        inpsConfiguration.getInps().getIseeConsultation().setBaseUrl("realBaseUrl");

        Assertions.assertEquals("mockBaseUrl", inpsConfiguration.getBaseUrlForInps());
    }

    @Test
    void givenMockDisabledThenBaseUrlIsReal() {
        inpsConfiguration.getInpsMock().setEnabledIsee(false);
        inpsConfiguration.getInpsMock().setBaseUrl("mockBaseUrl");
        inpsConfiguration.getInps().getIseeConsultation().setBaseUrl("realBaseUrl");

        Assertions.assertEquals("realBaseUrl", inpsConfiguration.getBaseUrlForInps());
    }

    @Test
    void givenNullInpsIseeConsultationWhenGetBaseUrlThenReturnNull() {
        inpsConfiguration.getInpsMock().setEnabledIsee(false);
        inpsConfiguration.getInps().setIseeConsultation(null);

        String baseUrl = inpsConfiguration.getBaseUrlForInps();

        Assertions.assertNull(baseUrl, "Expected null when iseeConsultation is null and mock disabled");
    }

    @Test
    void givenNullInpsConfigWhenGetConnectionTimeoutAndRequestTimeoutThenReturnNull() {
        inpsConfiguration.getInps().getIseeConsultation().setConfig(null);
        Assertions.assertNull(inpsConfiguration.getConnectionTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getRequestTimeoutForInps());
    }

    @Test
    void givenNullHeaderWhenGetOfficeCodeAndUserIdThenReturnNull() {
        inpsConfiguration.getInps().setHeader(null);
        Assertions.assertNull(inpsConfiguration.getOfficeCodeForInps());
        Assertions.assertNull(inpsConfiguration.getUserIdForInps());
    }

    @Test
    void givenNullSecureWhenGetCertAndKeyThenReturnNull() {
        inpsConfiguration.getInps().setSecure(null);
        Assertions.assertNull(inpsConfiguration.getCertForInps());
        Assertions.assertNull(inpsConfiguration.getKeyForInps());
    }

    @Test
    void givenInpsIsNullThenAllGettersReturnNull() {
        inpsConfiguration.getInpsMock().setEnabledIsee(false);
        inpsConfiguration.setInps(null);

        Assertions.assertNull(inpsConfiguration.getBaseUrlForInps());
        Assertions.assertNull(inpsConfiguration.getConnectionTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getRequestTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getOfficeCodeForInps());
        Assertions.assertNull(inpsConfiguration.getUserIdForInps());
        Assertions.assertNull(inpsConfiguration.getCertForInps());
        Assertions.assertNull(inpsConfiguration.getKeyForInps());
    }
}
