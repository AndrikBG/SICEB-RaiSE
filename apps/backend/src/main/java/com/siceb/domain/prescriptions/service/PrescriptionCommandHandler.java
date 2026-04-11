package com.siceb.domain.prescriptions.service;

import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.repository.ClinicalEventRepository;
import com.siceb.domain.clinicalcare.service.ClinicalEventStore;
import com.siceb.domain.prescriptions.command.CreatePrescriptionCommand;
import com.siceb.platform.branch.TenantContext;
import com.siceb.platform.iam.StaffContext;
import com.siceb.platform.iam.security.ResidencyLevelPolicy;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Handles prescription creation within a consultation context (US-031).
 * Residency-level restrictions enforced at service layer (SEC-01):
 * R1/R2/R3 cannot prescribe controlled medications regardless of entry point.
 */
@Service
public class PrescriptionCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionCommandHandler.class);
    private static final String CONTROLLED_MED_PERMISSION = "controlled_med:prescribe";

    private final ClinicalEventStore eventStore;
    private final ClinicalEventRepository eventRepository;
    private final ResidencyLevelPolicy residencyPolicy;

    public PrescriptionCommandHandler(ClinicalEventStore eventStore,
                                       ClinicalEventRepository eventRepository,
                                       ResidencyLevelPolicy residencyPolicy) {
        this.eventStore = eventStore;
        this.eventRepository = eventRepository;
        this.residencyPolicy = residencyPolicy;
    }

    @Transactional
    public UUID createPrescription(CreatePrescriptionCommand cmd) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.require();

        validateResidencyLevelForPrescription(cmd);

        boolean consultationExists = eventRepository.findById(cmd.consultationId())
                .filter(e -> e.getEventType() == ClinicalEventType.CONSULTATION)
                .isPresent();

        if (!consultationExists) {
            throw new ClinicalDomainException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Consultation " + cmd.consultationId() + " not found");
        }

        if (cmd.items().isEmpty()) {
            throw new ClinicalDomainException(ErrorCode.VALIDATION_FAILED,
                    "Prescription must have at least one item");
        }

        List<Map<String, Object>> itemPayloads = cmd.items().stream()
                .map(item -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("medicationId", item.medicationId().toString());
                    m.put("medicationName", item.medicationName());
                    m.put("quantity", item.quantity());
                    m.put("dosage", item.dosage());
                    m.put("frequency", item.frequency());
                    if (item.duration() != null) m.put("duration", item.duration());
                    if (item.route() != null) m.put("route", item.route());
                    if (item.instructions() != null) m.put("instructions", item.instructions());
                    if (item.isControlled()) m.put("isControlled", true);
                    return m;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prescriptionId", cmd.prescriptionId().toString());
        payload.put("consultationId", cmd.consultationId().toString());
        payload.put("status", "ACTIVE");
        payload.put("items", itemPayloads);

        ClinicalEvent event = ClinicalEvent.builder()
                .eventId(cmd.prescriptionId())
                .recordId(cmd.recordId())
                .eventType(ClinicalEventType.PRESCRIPTION)
                .branchId(branchId)
                .performedByStaffId(staffId)
                .idempotencyKey(cmd.idempotencyKey())
                .payload(payload)
                .build();

        ClinicalEvent saved = eventStore.append(event);
        log.info("Prescription created: id={}, consultation={}, items={}",
                cmd.prescriptionId(), cmd.consultationId(), cmd.items().size());
        return saved.getEventId();
    }

    /**
     * Validates residency-level restrictions at the service layer (SEC-01, G1 fix).
     * R1/R2/R3 are blocked from prescribing controlled medications regardless of
     * how the prescription is created (HTTP, sync replay, internal call).
     *
     * Phase 5 will add medication catalog lookup to determine isControlled from
     * the catalog rather than trusting client flags. For now, any prescription
     * by a blocked residency level that includes items flagged as controlled
     * is rejected. Items without the flag are treated as non-controlled.
     */
    private void validateResidencyLevelForPrescription(CreatePrescriptionCommand cmd) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SicebUserPrincipal principal)) {
            return; // No authenticated context (e.g., system operation) — skip
        }

        String residencyLevel = principal.residencyLevel();
        if (residencyLevel == null) {
            return; // Not a resident — no restrictions
        }

        boolean hasControlledItems = cmd.items().stream()
                .anyMatch(CreatePrescriptionCommand.PrescriptionItemDto::isControlled);

        if (hasControlledItems) {
            ResidencyLevelPolicy.EvaluationResult result = residencyPolicy.evaluate(
                    residencyLevel, CONTROLLED_MED_PERMISSION, true);

            if (!result.permitted()) {
                log.warn("Residency-level block: {} attempted controlled medication prescription (SEC-01)",
                        principal.username());
                throw new ClinicalDomainException(ErrorCode.RESIDENCY_RESTRICTED,
                        result.reason());
            }
        }
    }
}
