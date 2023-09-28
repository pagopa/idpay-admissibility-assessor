package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.utils.RestTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class AnprC001RestClientSSLKOTest {

    @BeforeEach
    void setUp() {
        RestTestUtils.USE_TRUSTSTORE_KO = true;
        BaseIntegrationTest.initServerWiremock();
    }

    @AfterEach
    void clean() {
        RestTestUtils.USE_TRUSTSTORE_KO = false;
        BaseIntegrationTest.initServerWiremock();
    }

    @Test
    void testKo(){
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(AnprC001RestClientImplTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        Assertions.assertEquals(1,listener.getSummary().getTestsFailedCount());
    }
}