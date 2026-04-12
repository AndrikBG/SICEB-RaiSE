package com.siceb.platform.audit.service;

import com.siceb.platform.audit.entity.AuditLogEntry;
import com.siceb.platform.audit.repository.AuditLogEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Audit query operations per requeriments3.md §8.3:
 * - GetAuditTrailForEntity
 * - GetAuditTrailForUser
 * - GetAccessLogForPatient (LFPDPPP)
 * - VerifyChainIntegrity (IC-03)
 */
@Service
public class AuditQueryService {

    private final AuditLogEntryRepository repository;

    public AuditQueryService(AuditLogEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> getAuditTrailForEntity(String entityType, UUID entityId,
            Instant from, Instant to, Pageable pageable) {
        return repository.findByTargetEntityAndTargetIdAndCreatedAtBetween(
                entityType, entityId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> getAuditTrailForUser(UUID userId,
            Instant from, Instant to, Pageable pageable) {
        return repository.findByUserIdAndCreatedAtBetween(userId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> getAccessLogForPatient(UUID patientId,
            Instant from, Instant to, Pageable pageable) {
        return repository.findByTargetEntityAndTargetIdAndCreatedAtBetween(
                "Patient", patientId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public ChainIntegrityResult verifyChainIntegrity(UUID fromEntryId, UUID toEntryId) {
        List<AuditLogEntry> entries = repository.findChainBetween(fromEntryId, toEntryId);
        if (entries.isEmpty()) {
            return new ChainIntegrityResult(true, 0, List.of());
        }

        List<String> violations = new ArrayList<>();

        AuditLogEntry first = entries.get(0);
        if (first.getPreviousHash() == null) {
            violations.add("Entry " + first.getEntryId() + " has null previous_hash");
        }

        for (int i = 1; i < entries.size(); i++) {
            AuditLogEntry current = entries.get(i);
            AuditLogEntry previous = entries.get(i - 1);
            if (!previous.getEntryHash().equals(current.getPreviousHash())) {
                violations.add("Chain broken at entry " + current.getEntryId()
                        + ": expected previous_hash=" + previous.getEntryHash()
                        + " but found " + current.getPreviousHash());
            }
        }

        return new ChainIntegrityResult(violations.isEmpty(), entries.size(), violations);
    }

    public record ChainIntegrityResult(
            boolean valid,
            int entriesVerified,
            List<String> violations
    ) {}
}
