package it.gov.pagopa.admissibility.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    @Autowired
    private InpsConfiguration inpsConfiguration;

    @Test
    void givenPropertiesWhenGetInpsThenReturnInfo() {
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        Assertions.assertNotNull(inps);

        String expectedBaseUrl = inps.getIseeConsultation().isMockEnabled()
                ? inps.getIseeConsultation().getMockBaseUrl()
                : inps.getIseeConsultation().getBaseUrl();

        Assertions.assertEquals(expectedBaseUrl, inps.getIseeConsultation().getBaseUrl());
        Assertions.assertEquals(10, inps.getIseeConsultation().getConfig().getConnectionTimeout());
        Assertions.assertEquals(20, inps.getIseeConsultation().getConfig().getRequestTimeout());
        Assertions.assertEquals("usertest", inps.getHeader().getUserId());
        Assertions.assertEquals("testOfficeCode", inps.getHeader().getOfficeCode());
        Assertions.assertEquals("testCert", inps.getSecure().getCert());
        Assertions.assertEquals("testKey", inps.getSecure().getKey());
    }

    @Test
    void givenMockEnabledFalseThenBaseUrlIsReal() {
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.getIseeConsultation().setMockEnabled(false);

        String baseUrlForInps = inps.getIseeConsultation().getBaseUrl();
        Assertions.assertEquals("testRealBaseUrl", baseUrlForInps);
    }

    @Test
    void givenMockEnabledTrueThenBaseUrlIsMock() {
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        InpsConfiguration.IseeConsultation ic = inps.getIseeConsultation();

        ic.setMockBaseUrl("testMockBaseUrl");
        ic.setRealBaseUrl("testRealBaseUrl");
        ic.setMockEnabled(true);

        ic.init();

        Assertions.assertEquals("testMockBaseUrl", ic.getBaseUrl());
    }

    @Test
    void givenNullInpsIseeConsultationWhenGetBaseUrlThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setIseeConsultation(null);
        Assertions.assertNull(inpsConfiguration.getBaseUrlForInps());
    }

    @Test
    void givenNullInpsConfigWhenGetConnectionTimeoutAndRequestTimeoutThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.getIseeConsultation().setConfig(null);
        Assertions.assertNull(inpsConfiguration.getConnectionTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getRequestTimeoutForInps());
    }

    @Test
    void givenNullHeaderWhenGetOfficeCodeAndUserIdThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setHeader(null);
        Assertions.assertNull(inpsConfiguration.getOfficeCodeForInps());
        Assertions.assertNull(inpsConfiguration.getUserIdForInps());
    }

    @Test
    void givenNullSecureWhenGetCertAndKeyThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setSecure(null);
        Assertions.assertNull(inpsConfiguration.getCertForInps());
        Assertions.assertNull(inpsConfiguration.getKeyForInps());
    }

    @Test
    void givenNullInpsWhenGetAllThenReturnNull(){
        InpsConfiguration.Inps inps = inpsConfiguration.getInps();
        inps.setIseeConsultation(null);
        inps.setHeader(null);
        inps.setSecure(null);

        Assertions.assertNull(inpsConfiguration.getBaseUrlForInps());
        Assertions.assertNull(inpsConfiguration.getConnectionTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getRequestTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getOfficeCodeForInps());
        Assertions.assertNull(inpsConfiguration.getUserIdForInps());
        Assertions.assertNull(inpsConfiguration.getCertForInps());
        Assertions.assertNull(inpsConfiguration.getKeyForInps());
    }

    @Test
    void givenInpsIsNullThenAllGettersReturnNull() {
        inpsConfiguration.setInps(null);

        Assertions.assertNull(inpsConfiguration.getBaseUrlForInps());
        Assertions.assertNull(inpsConfiguration.getConnectionTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getRequestTimeoutForInps());
        Assertions.assertNull(inpsConfiguration.getOfficeCodeForInps());
        Assertions.assertNull(inpsConfiguration.getUserIdForInps());
        Assertions.assertNull(inpsConfiguration.getCertForInps());
        Assertions.assertNull(inpsConfiguration.getKeyForInps());
    }

    @Test
    void givenMockEnabledTrueThenInitSetsBaseUrl() {
        InpsConfiguration.IseeConsultation ic = new InpsConfiguration.IseeConsultation();
        ic.setMockEnabled(true);
        ic.setMockBaseUrl("mockUrl");
        ic.setRealBaseUrl("realUrl");

        ic.init();

        Assertions.assertEquals("mockUrl", ic.getBaseUrl());
    }

    @Test
    void givenMockEnabledFalseThenInitSetsBaseUrl() {
        InpsConfiguration.IseeConsultation ic = new InpsConfiguration.IseeConsultation();
        ic.setMockEnabled(false);
        ic.setMockBaseUrl("mockUrl");
        ic.setRealBaseUrl("realUrl");

        ic.init();

        Assertions.assertEquals("realUrl", ic.getBaseUrl());
    }

    @Test
    void givenMockEnabledTrueThenGetBaseUrlForInpsReturnsMock() {
        InpsConfiguration.IseeConsultation ic = new InpsConfiguration.IseeConsultation();
        ic.setMockEnabled(true);
        ic.setMockBaseUrl("mockUrl");
        ic.setRealBaseUrl("realUrl");

        InpsConfiguration.Inps inps = new InpsConfiguration.Inps();
        inps.setIseeConsultation(ic);
        inpsConfiguration.setInps(inps);

        Assertions.assertEquals("mockUrl", inpsConfiguration.getBaseUrlForInps());
    }

    @Test
    void givenMockEnabledFalseThenGetBaseUrlForInpsReturnsReal() {
        InpsConfiguration.IseeConsultation ic = new InpsConfiguration.IseeConsultation();
        ic.setMockEnabled(false);
        ic.setMockBaseUrl("mockUrl");
        ic.setRealBaseUrl("realUrl");

        InpsConfiguration.Inps inps = new InpsConfiguration.Inps();
        inps.setIseeConsultation(ic);
        inpsConfiguration.setInps(inps);

        Assertions.assertEquals("realUrl", inpsConfiguration.getBaseUrlForInps());
    }
}
