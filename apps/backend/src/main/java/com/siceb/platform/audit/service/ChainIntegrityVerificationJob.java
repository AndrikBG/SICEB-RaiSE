package com.siceb.platform.audit.service;

import com.siceb.platform.audit.entity.AuditLogEntry;
import com.siceb.platform.audit.repository.AuditLogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodic audit chain integrity verification (T3.5.5).
 * Runs daily at 3:00 AM by default. Checks the last 1000 entries
 * for hash chain continuity and logs an alert on any corruption.
 */
@Component
public class ChainIntegrityVerificationJob {

    private static final Logger log = LoggerFactory.getLogger(ChainIntegrityVerificationJob.class);
    private static final int VERIFICATION_BATCH_SIZE = 1000;

    private final AuditQueryService auditQueryService;
    private final AuditLogEntryRepository repository;

    public ChainIntegrityVerificationJob(AuditQueryService auditQueryService,
            AuditLogEntryRepository repository) {
        this.auditQueryService = auditQueryService;
        this.repository = repository;
    }

    @Scheduled(cron = "${audit.chain-verification.cron:0 0 3 * * ?}")
    public void verifyIntegrity() {
        log.info("Starting audit chain integrity verification");

        List<AuditLogEntry> recentEntries = repository.findLastEntries(
                PageRequest.of(0, VERIFICATION_BATCH_SIZE));

        if (recentEntries.isEmpty()) {
            log.info("No audit entries to verify");
            return;
        }

        AuditLogEntry newest = recentEntries.get(0);
        AuditLogEntry oldest = recentEntries.get(recentEntries.size() - 1);

        AuditQueryService.ChainIntegrityResult result =
                auditQueryService.verifyChainIntegrity(oldest.getEntryId(), newest.getEntryId());

        if (result.valid()) {
            log.info("Audit chain integrity verified: {} entries checked", result.entriesVerified());
        } else {
            log.error("AUDIT CHAIN INTEGRITY VIOLATION: {} entries checked, {} violations: {}",
                    result.entriesVerified(), result.violations().size(), result.violations());
        }
    }
}
