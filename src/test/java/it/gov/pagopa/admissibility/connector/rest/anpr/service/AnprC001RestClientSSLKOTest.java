package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class AnprC001RestClientSSLKOTest {

    @BeforeAll
    static void setUp() {
        BaseIntegrationTest.configureServerWiremockBeforeAll(true, false);
    }

    @Test
    void testKo(){
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(AnprC001RestClientImplIntegrationTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        Assertions.assertEquals(1,listener.getSummary().getTestsFailedCount());
    }
}