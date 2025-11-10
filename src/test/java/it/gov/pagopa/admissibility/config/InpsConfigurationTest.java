package it.gov.pagopa.admissibility.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class InpsConfigurationTest {

    private InpsConfiguration config;

    @BeforeEach
    void setUp() {
        config = new InpsConfiguration();

        InpsConfiguration.InpsMock mock = new InpsConfiguration.InpsMock();
        mock.setEnabledIsee(true);
        mock.setBaseUrl("http://mock.inps.it");
        config.setInpsMock(mock);

        InpsConfiguration.Inps inps = new InpsConfiguration.Inps();

        InpsConfiguration.IseeConsultation consultation = new InpsConfiguration.IseeConsultation();
        consultation.setBaseUrl("http://real.inps.it");

        InpsConfiguration.Config timeouts = new InpsConfiguration.Config();
        timeouts.setConnectionTimeout(1000);
        timeouts.setRequestTimeout(2000);
        consultation.setConfig(timeouts);
        inps.setIseeConsultation(consultation);

        InpsConfiguration.Header header = new InpsConfiguration.Header();
        header.setOfficeCode("OFF123");
        header.setUserId("USR456");
        inps.setHeader(header);

        InpsConfiguration.Secure secure = new InpsConfiguration.Secure();
        String cert = Base64.getEncoder().encodeToString("CERTIFICATE".getBytes());
        secure.setCert(cert);
        secure.setKey("PRIVATE_KEY");
        inps.setSecure(secure);

        config.setInps(inps);
    }

    @Test
    void testGetBaseUrlForInps_MockEnabled() {
        assertEquals("http://mock.inps.it", config.getBaseUrlForInps());
    }

    @Test
    void testGetConnectionTimeoutForInps() {
        assertEquals(1000, config.getConnectionTimeoutForInps());
    }

    @Test
    void testGetRequestTimeoutForInps() {
        assertEquals(2000, config.getRequestTimeoutForInps());
    }

    @Test
    void testGetOfficeCodeForInps() {
        assertEquals("OFF123", config.getOfficeCodeForInps());
    }

    @Test
    void testGetUserIdForInps() {
        assertEquals("USR456", config.getUserIdForInps());
    }

    @Test
    void testGetCertForInps() {
        assertEquals("CERTIFICATE", config.getCertForInps());
    }

    @Test
    void testGetKeyForInps() {
        assertEquals("PRIVATE_KEY", config.getKeyForInps());
    }

    @Test
    void testSetCertWithInvalidBase64() {
        InpsConfiguration.Secure secure = new InpsConfiguration.Secure();
        secure.setCert("INVALID_BASE64");
        assertNull(secure.getCert());
    }

    @Test
    void testSetCertWithNull() {
        InpsConfiguration.Secure secure = new InpsConfiguration.Secure();
        secure.setCert(null);
        assertNull(secure.getCert());
    }


}