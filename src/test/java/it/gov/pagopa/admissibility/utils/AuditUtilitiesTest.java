package it.gov.pagopa.admissibility.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.AuditLogger;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditUtilitiesTest {
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String FAMILY_ID = "TEST_FAMILY_ID";

    private final AuditUtilities auditUtilities = new AuditUtilities();
    private MemoryAppender memoryAppender;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void logDeletedDroolsRule_ok(){
        auditUtilities.logDeletedDroolsRule(INITIATIVE_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=AdmissibilityAssessor dstip=%s msg=Drools rule deleted." +
                        " cs1Label=initiativeId cs1=%s")
                        .formatted(
                                AuditLogger.SRCIP,
                                INITIATIVE_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logDeletedInitiativeCounters_ok(){
        auditUtilities.logDeletedInitiativeCounters(INITIATIVE_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=AdmissibilityAssessor dstip=%s msg=Initiative counters deleted." +
                        " cs1Label=initiativeId cs1=%s")
                        .formatted(
                                AuditLogger.SRCIP,
                                INITIATIVE_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logDeletedOnboardingFamilies_ok(){
        auditUtilities.logDeletedOnboardingFamilies(FAMILY_ID, INITIATIVE_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=AdmissibilityAssessor dstip=%s msg=Onboarded families deleted." +
                        " cs1Label=familyId cs1=%s cs2Label=initiativeId cs2=%s")
                        .formatted(
                                AuditLogger.SRCIP,
                                FAMILY_ID,
                                INITIATIVE_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logAnprFamilies_ok(){
        String userId = "USER_ID";
        String operationId = "OPERATION_ID";
        String timestamp = "2025-10-21T14:27:45.008906930";
        auditUtilities.logAnprFamilies(userId, INITIATIVE_ID, operationId, timestamp);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=AdmissibilityAssessor dstip=%s msg=ANPR families retrieve." +
                        " cs1Label=userId cs1=%s cs2Label=initiativeId cs2=%s cs3Label=operationAnprId cs3=%s cs4Label=timestamp cs4=%s")
                        .formatted(
                                AuditLogger.SRCIP,
                                userId,
                                INITIATIVE_ID,
                                operationId,
                                timestamp
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
