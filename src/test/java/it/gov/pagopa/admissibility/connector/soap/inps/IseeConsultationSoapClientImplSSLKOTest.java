package it.gov.pagopa.admissibility.connector.soap.inps;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class IseeConsultationSoapClientImplSSLKOTest{

    @BeforeAll
    static void setUp() {
        BaseIntegrationTest.configureServerWiremockBeforeAll(true, false);
    }

    @Test
    void testKo(){
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(IseeConsultationSoapClientImplTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        Assertions.assertEquals(2,listener.getSummary().getTestsFailedCount());
    }
}