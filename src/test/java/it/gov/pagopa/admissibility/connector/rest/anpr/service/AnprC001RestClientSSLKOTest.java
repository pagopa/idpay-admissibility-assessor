package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.common.reactive.wireMock.BaseWireMockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

class AnprC001RestClientSSLKOTest {

    @BeforeAll
    static void setUp() {
        BaseWireMockTest.configureServerWiremockBeforeAll(true, false);
    }

    @Test
    void testKo(){
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(AnprC001RestClientImplIntegrationTest.class, "getResidenceAssessment"))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        Assertions.assertEquals(1,listener.getSummary().getTestsFailedCount());
    }
}