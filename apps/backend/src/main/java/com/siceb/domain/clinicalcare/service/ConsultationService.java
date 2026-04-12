package com.siceb.domain.clinicalcare.service;

import com.siceb.domain.clinicalcare.command.AddConsultationCommand;
import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.repository.MedicalRecordRepository;
import com.siceb.platform.branch.TenantContext;
import com.siceb.platform.iam.StaffContext;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ConsultationAggregate — handles adding consultations to a medical record.
 * Captures SOAP format, vital signs, and supervision flag for R1/R2 residents (USA-02).
 */
@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);

    private final MedicalRecordRepository recordRepository;
    private final ClinicalEventStore eventStore;

    public ConsultationService(MedicalRecordRepository recordRepository,
                               ClinicalEventStore eventStore) {
        this.recordRepository = recordRepository;
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID addConsultation(AddConsultationCommand cmd) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.require();

        if (!recordRepository.existsById(cmd.recordId())) {
            throw new ClinicalDomainException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Medical record " + cmd.recordId() + " not found");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consultationId", cmd.consultationId().toString());
        payload.put("subjective", cmd.subjective());
        payload.put("objective", cmd.objective());
        payload.put("diagnosis", cmd.diagnosis());
        if (cmd.diagnosisCode() != null) payload.put("diagnosisCode", cmd.diagnosisCode());
        payload.put("plan", cmd.plan());
        if (cmd.vitalSigns() != null) payload.put("vitalSigns", cmd.vitalSigns());
        payload.put("requiresSupervision", cmd.requiresSupervision());
        if (cmd.supervisorStaffId() != null) {
            payload.put("supervisorStaffId", cmd.supervisorStaffId().toString());
        }

        ClinicalEvent event = ClinicalEvent.builder()
                .eventId(cmd.consultationId())
                .recordId(cmd.recordId())
                .eventType(ClinicalEventType.CONSULTATION)
                .branchId(branchId)
                .performedByStaffId(staffId)
                .idempotencyKey(cmd.idempotencyKey())
                .payload(payload)
                .build();

        ClinicalEvent saved = eventStore.append(event);
        log.info("Consultation added: id={}, record={}, branch={}", cmd.consultationId(), cmd.recordId(), branchId);
        return saved.getEventId();
    }
}
