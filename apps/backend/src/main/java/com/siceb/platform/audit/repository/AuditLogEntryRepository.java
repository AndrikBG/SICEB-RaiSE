package com.siceb.platform.audit.repository;

import com.siceb.platform.audit.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {

    Page<AuditLogEntry> findByTargetEntityAndTargetIdAndCreatedAtBetween(
            String targetEntity, UUID targetId, Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntry> findByUserIdAndCreatedAtBetween(
            UUID userId, Instant from, Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntry a WHERE a.createdAt >= " +
            "(SELECT b.createdAt FROM AuditLogEntry b WHERE b.entryId = :fromId) " +
            "AND a.createdAt <= (SELECT c.createdAt FROM AuditLogEntry c WHERE c.entryId = :toId) " +
            "ORDER BY a.createdAt ASC, a.entryId ASC")
    List<AuditLogEntry> findChainBetween(@Param("fromId") UUID fromId, @Param("toId") UUID toId);

    @Query("SELECT a FROM AuditLogEntry a ORDER BY a.createdAt DESC, a.entryId DESC")
    List<AuditLogEntry> findLastEntries(Pageable pageable);
}
