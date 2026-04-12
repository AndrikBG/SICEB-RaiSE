package com.siceb.api;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.platform.consent.entity.ArcoRequest;
import com.siceb.platform.consent.entity.ConsentRecord;
import com.siceb.platform.consent.service.LfpdpppComplianceTracker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for LFPDPPP compliance (CRN-32, US-066).
 * Consent management, ARCO workflows, and corrective addenda.
 */
@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private final LfpdpppComplianceTracker complianceTracker;

    public ConsentController(LfpdpppComplianceTracker complianceTracker) {
        this.complianceTracker = complianceTracker;
    }

    @PostMapping("/grant")
    @PreAuthorize("@auth.check('consent:manage')")
    public ResponseEntity<ConsentRecord> grantConsent(@Valid @RequestBody GrantConsentRequest request) {
        ConsentRecord consent = complianceTracker.grantConsent(
                request.patientId(), request.consentType(), request.purpose());
        return ResponseEntity.status(HttpStatus.CREATED).body(consent);
    }

    @PostMapping("/{consentId}/revoke")
    @PreAuthorize("@auth.check('consent:manage')")
    public ResponseEntity<ConsentRecord> revokeConsent(@PathVariable UUID consentId) {
        ConsentRecord consent = complianceTracker.revokeConsent(consentId);
        return ResponseEntity.ok(consent);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("@auth.check('consent:read')")
    public ResponseEntity<List<ConsentRecord>> getPatientConsents(@PathVariable UUID patientId) {
        return ResponseEntity.ok(complianceTracker.getConsentStatus(patientId));
    }

    @PostMapping("/arco")
    @PreAuthorize("@auth.check('arco:manage')")
    public ResponseEntity<ArcoRequest> createArcoRequest(@Valid @RequestBody CreateArcoRequest request) {
        ArcoRequest arcoRequest = complianceTracker.createArcoRequest(
                request.patientId(), request.requestType(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(arcoRequest);
    }

    @PostMapping("/arco/{requestId}/process")
    @PreAuthorize("@auth.check('arco:manage')")
    public ResponseEntity<ArcoRequest> processArcoRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ProcessArcoRequest request
    ) {
        ArcoRequest result = complianceTracker.processArcoRequest(
                requestId, request.resolution(), request.approved());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/arco/pending")
    @PreAuthorize("@auth.check('arco:read')")
    public ResponseEntity<Page<ArcoRequest>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(complianceTracker.getPendingArcoRequests(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"))));
    }

    @GetMapping("/arco/patient/{patientId}")
    @PreAuthorize("@auth.check('arco:read')")
    public ResponseEntity<List<ArcoRequest>> getPatientArcoRequests(@PathVariable UUID patientId) {
        return ResponseEntity.ok(complianceTracker.getPatientArcoRequests(patientId));
    }

    @PostMapping("/rectification")
    @PreAuthorize("@auth.check('arco:manage')")
    public ResponseEntity<ClinicalEvent> createCorrectiveAddendum(
            @Valid @RequestBody CorrectiveAddendumRequest request
    ) {
        ClinicalEvent event = complianceTracker.createCorrectiveAddendum(
                request.recordId(), request.originalEventId(),
                request.correctionData(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    public record GrantConsentRequest(
            @NotNull UUID patientId,
            @NotBlank String consentType,
            @NotBlank String purpose
    ) {}

    public record CreateArcoRequest(
            @NotNull UUID patientId,
            @NotNull ArcoRequest.ArcoType requestType,
            @NotBlank String description
    ) {}

    public record ProcessArcoRequest(
            @NotBlank String resolution,
            boolean approved
    ) {}

    public record CorrectiveAddendumRequest(
            @NotNull UUID recordId,
            @NotNull UUID originalEventId,
            @NotBlank String correctionData,
            @NotBlank String reason
    ) {}
}
