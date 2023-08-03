package it.gov.pagopa.admissibility.utils;

import it.gov.pagopa.common.utils.AuditLogger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=AdmissibilityAssessor dstip=%s", AuditLogger.SRCIP);
    private static final String CEF_FAMILY_PATTERN_DELETE = CEF + " msg={} cs1Label=familyId cs1={} cs2Label=initiativeId cs2={}";
    private static final String CEF_PATTERN_DELETE = CEF + " msg={} cs1Label=initiativeId cs1={}";

    public void logDeletedDroolsRule(String initiativeId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_DELETE,
                "Drools rule deleted.", initiativeId
        );
    }
    public void logDeletedInitiativeCounters(String initiativeId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_DELETE,
                "Initiative counters deleted.", initiativeId
        );
    }
    public void logDeletedOnboardingFamilies(String familyId, String initiativeId) {
        AuditLogger.logAuditString(
                CEF_FAMILY_PATTERN_DELETE,
                "Onboarded families deleted.", familyId, initiativeId
        );
    }
}
