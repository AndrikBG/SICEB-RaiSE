package com.siceb.platform.consent.service;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.service.ClinicalEventStore;
import com.siceb.platform.branch.TenantContext;
import com.siceb.platform.consent.entity.ArcoRequest;
import com.siceb.platform.consent.entity.ConsentRecord;
import com.siceb.platform.consent.repository.ArcoRequestRepository;
import com.siceb.platform.consent.repository.ConsentRecordRepository;
import com.siceb.platform.iam.StaffContext;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LFPDPPP compliance per requeriments3.md §5.3 and CRN-32:
 * - Consent lifecycle via consent_records
 * - ARCO workflows with 20-business-day legal deadlines
 * - Corrective addendum for Rectification on immutable clinical records
 */
@Service
public class LfpdpppComplianceTracker {

    private static final Logger log = LoggerFactory.getLogger(LfpdpppComplianceTracker.class);
    private static final int ARCO_DEADLINE_BUSINESS_DAYS = 20;

    private final ConsentRecordRepository consentRepository;
    private final ArcoRequestRepository arcoRepository;
    private final ClinicalEventStore clinicalEventStore;

    public LfpdpppComplianceTracker(
            ConsentRecordRepository consentRepository,
            ArcoRequestRepository arcoRepository,
            ClinicalEventStore clinicalEventStore
    ) {
        this.consentRepository = consentRepository;
        this.arcoRepository = arcoRepository;
        this.clinicalEventStore = clinicalEventStore;
    }

    @Transactional
    public ConsentRecord grantConsent(UUID patientId, String consentType, String purpose) {
        UUID branchId = TenantContext.require();
        UUID userId = StaffContext.get().orElse(null);

        ConsentRecord consent = new ConsentRecord(patientId, branchId, consentType, purpose, userId);
        ConsentRecord saved = consentRepository.save(consent);
        log.info("Consent granted: patient={}, type={}", patientId, consentType);
        return saved;
    }

    @Transactional
    public ConsentRecord revokeConsent(UUID consentId) {
        ConsentRecord consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentException(ErrorCode.RESOURCE_NOT_FOUND, "Consent record not found"));
        UUID userId = StaffContext.get().orElse(null);
        consent.revoke(userId);
        ConsentRecord saved = consentRepository.save(consent);
        log.info("Consent revoked: consent={}, patient={}", consentId, consent.getPatientId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> getConsentStatus(UUID patientId) {
        return consentRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> getActiveConsents(UUID patientId) {
        return consentRepository.findByPatientIdAndRevokedAtIsNull(patientId);
    }

    @Transactional
    public ArcoRequest createArcoRequest(UUID patientId, ArcoRequest.ArcoType requestType,
                                          String description) {
        UUID branchId = TenantContext.require();
        LocalDate deadline = calculateDeadline(ARCO_DEADLINE_BUSINESS_DAYS);
        ArcoRequest request = new ArcoRequest(patientId, branchId, requestType, description, deadline);
        ArcoRequest saved = arcoRepository.save(request);
        log.info("ARCO request created: type={}, patient={}, deadline={}", requestType, patientId, deadline);
        return saved;
    }

    @Transactional
    public ArcoRequest processArcoRequest(UUID requestId, String resolution, boolean approved) {
        ArcoRequest request = arcoRepository.findById(requestId)
                .orElseThrow(() -> new ConsentException(ErrorCode.RESOURCE_NOT_FOUND, "ARCO request not found"));
        UUID userId = StaffContext.get().orElse(null);
        request.startProcessing(userId);
        if (approved) {
            request.complete(resolution);
        } else {
            request.reject(resolution);
        }
        ArcoRequest saved = arcoRepository.save(request);
        log.info("ARCO request processed: id={}, status={}", requestId, request.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ArcoRequest> getPendingArcoRequests(Pageable pageable) {
        return arcoRepository.findByStatusIn(
                List.of(ArcoRequest.ArcoStatus.PENDING, ArcoRequest.ArcoStatus.IN_PROGRESS),
                pageable);
    }

    @Transactional(readOnly = true)
    public List<ArcoRequest> getPatientArcoRequests(UUID patientId) {
        return arcoRepository.findByPatientId(patientId);
    }

    /**
     * Corrective addendum for Rectification (T3.6.3).
     * Appends a CORRECTIVE_ADDENDUM event to the immutable clinical event store
     * referencing the original event and providing the correction.
     */
    @Transactional
    public ClinicalEvent createCorrectiveAddendum(UUID recordId, UUID originalEventId,
                                                    String correctionData, String reason) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.get().orElse(null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("originalEventId", originalEventId.toString());
        payload.put("correction", correctionData);
        payload.put("reason", reason);
        if (staffId != null) {
            payload.put("correctedBy", staffId.toString());
        }

        String idempotencyKey = "ADDENDUM-" + originalEventId + "-" + UUID.randomUUID();

        ClinicalEvent addendum = ClinicalEvent.builder()
                .eventId(UUID.randomUUID())
                .recordId(recordId)
                .branchId(branchId)
                .eventType(ClinicalEventType.CORRECTIVE_ADDENDUM)
                .performedByStaffId(staffId)
                .idempotencyKey(idempotencyKey)
                .payload(payload)
                .build();

        ClinicalEvent saved = clinicalEventStore.append(addendum);
        log.info("Corrective addendum created: record={}, originalEvent={}", recordId, originalEventId);
        return saved;
    }

    private LocalDate calculateDeadline(int businessDays) {
        LocalDate date = LocalDate.now();
        int addedDays = 0;
        while (addedDays < businessDays) {
            date = date.plusDays(1);
            if (date.getDayOfWeek().getValue() <= 5) {
                addedDays++;
            }
        }
        return date;
    }

    public static class ConsentException extends RuntimeException {
        private final ErrorCode errorCode;

        public ConsentException(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() { return errorCode; }
    }
}
