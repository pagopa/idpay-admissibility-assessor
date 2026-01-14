package it.gov.pagopa.common.reactive.pdnd.service;

import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceConfig;
import it.gov.pagopa.common.reactive.pdnd.config.BasePdndServiceProviderConfig;
import it.gov.pagopa.common.reactive.pdnd.dto.PdndServiceConfig;
import it.gov.pagopa.common.utils.TestUtils;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class BasePdndServiceTest {

    private static class DummyPdndServiceProviderConfig extends BasePdndServiceProviderConfig {
    }

    private static class DummyPdndServiceConfig extends BasePdndServiceConfig {
    }

    @Test
    void testBuildDefaultPdndServiceConfig() {
        // Given
        DummyPdndServiceProviderConfig serviceProviderConfig = buildPdndServiceProviderConfig();
        DummyPdndServiceConfig serviceConfig = buildPdndServiceConfig();

        // When
        PdndServiceConfig<String, String> result = BasePdndService.buildDefaultPdndServiceConfig(serviceProviderConfig, serviceConfig, String.class, String.class);

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertSame(serviceProviderConfig.getBaseUrl(), result.getBaseUrl());
        Assertions.assertEquals(serviceProviderConfig.getAuthExpirationSeconds(), result.getAuthExpirationSeconds());
        Assertions.assertSame(serviceProviderConfig.getHttpsConfig(), result.getHttpsConfig());
        Assertions.assertSame(serviceProviderConfig.getAgidConfig(), result.getAgidConfig());
        Assertions.assertSame(serviceConfig.getAudience(), result.getAudience());
        Assertions.assertSame(serviceConfig.getHttpMethod(), result.getHttpMethod());
        Assertions.assertSame(serviceConfig.getPath(), result.getPath());
        Assertions.assertEquals("", result.getEmptyResponseBody());

        TestUtils.checkNotNullFields(result);
    }

    private static DummyPdndServiceConfig buildPdndServiceConfig() {
        DummyPdndServiceConfig out = new DummyPdndServiceConfig();

        out.setAudience("Audience");
        out.setHttpMethod(HttpMethod.GET);
        out.setPath("Path");

        TestUtils.checkNotNullFields(out);
        return out;
    }

    private static DummyPdndServiceProviderConfig buildPdndServiceProviderConfig() {
        DummyPdndServiceProviderConfig out = new DummyPdndServiceProviderConfig();
        out.setBaseUrl("BASEURL");
        out.setAuthExpirationSeconds(300L);
        out.setHttpsConfig(buildHttpsConfig());
        out.setAgidConfig(buildAgidConfig());

        TestUtils.checkNotNullFields(out);
        return out;
    }

    @NonNull
    private static BasePdndServiceProviderConfig.AgidConfig buildAgidConfig() {
        BasePdndServiceProviderConfig.AgidConfig out = new BasePdndServiceProviderConfig.AgidConfig();

        out.setEnv("Env");
        out.setUserId("UserId");

        TestUtils.checkNotNullFields(out);
        return out;
    }

    @NonNull
    private static BasePdndServiceProviderConfig.HttpsConfig buildHttpsConfig() {
        BasePdndServiceProviderConfig.HttpsConfig out = new BasePdndServiceProviderConfig.HttpsConfig();

        out.setEnabled(true);
        out.setCert("Cert");
        out.setKey("Key");
        out.setMutualAuthEnabled(true);
        out.setTrustCertificatesCollection("TrustCertificatesCollection");

        TestUtils.checkNotNullFields(out);
        return out;
    }

}
