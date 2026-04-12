package com.siceb.api;

import com.siceb.platform.audit.entity.AuditLogEntry;
import com.siceb.platform.audit.service.AuditQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST endpoints for audit queries per requeriments3.md §8.3.
 * All operations are branch-scoped via RLS; Director General
 * uses admin_reporting BYPASSRLS for cross-branch queries.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final String SORT_BY_CREATED_AT = "createdAt";

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("@auth.check('audit:read')")
    public ResponseEntity<Page<AuditLogEntry>> getEntityTrail(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        Page<AuditLogEntry> result = auditQueryService.getAuditTrailForEntity(
                entityType, entityId, effectiveFrom, effectiveTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_BY_CREATED_AT)));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@auth.check('audit:read')")
    public ResponseEntity<Page<AuditLogEntry>> getUserTrail(
            @PathVariable UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        Page<AuditLogEntry> result = auditQueryService.getAuditTrailForUser(
                userId, effectiveFrom, effectiveTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_BY_CREATED_AT)));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patient/{patientId}/access")
    @PreAuthorize("@auth.check('audit:read')")
    public ResponseEntity<Page<AuditLogEntry>> getPatientAccessLog(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        Page<AuditLogEntry> result = auditQueryService.getAccessLogForPatient(
                patientId, effectiveFrom, effectiveTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_BY_CREATED_AT)));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/verify")
    @PreAuthorize("@auth.check('audit:verify')")
    public ResponseEntity<AuditQueryService.ChainIntegrityResult> verifyChain(
            @RequestParam UUID fromEntryId,
            @RequestParam UUID toEntryId
    ) {
        AuditQueryService.ChainIntegrityResult result =
                auditQueryService.verifyChainIntegrity(fromEntryId, toEntryId);
        return ResponseEntity.ok(result);
    }
}
