package com.siceb.domain.laboratory.service;

import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.repository.PatientRepository;
import com.siceb.domain.clinicalcare.repository.ClinicalEventRepository;
import com.siceb.domain.clinicalcare.repository.MedicalRecordRepository;
import com.siceb.domain.clinicalcare.service.ClinicalEventStore;
import com.siceb.domain.laboratory.command.CreateLabStudiesCommand;
import com.siceb.domain.laboratory.command.RecordLabResultCommand;
import com.siceb.domain.laboratory.model.PendingLabStudy;
import com.siceb.domain.laboratory.model.StudyStatus;
import com.siceb.domain.laboratory.repository.PendingLabStudyRepository;
import com.siceb.platform.branch.TenantContext;
import com.siceb.platform.iam.StaffContext;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Handles lab study orders and result recording (US-038, US-040, US-041).
 * Each study becomes a LAB_ORDER event; results become LAB_RESULT events.
 */
@Service
public class LabStudyCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(LabStudyCommandHandler.class);

    private final ClinicalEventStore eventStore;
    private final ClinicalEventRepository eventRepository;
    private final MedicalRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final PendingLabStudyRepository pendingRepository;

    public LabStudyCommandHandler(ClinicalEventStore eventStore,
                                   ClinicalEventRepository eventRepository,
                                   MedicalRecordRepository recordRepository,
                                   PatientRepository patientRepository,
                                   PendingLabStudyRepository pendingRepository) {
        this.eventStore = eventStore;
        this.eventRepository = eventRepository;
        this.recordRepository = recordRepository;
        this.patientRepository = patientRepository;
        this.pendingRepository = pendingRepository;
    }

    @Transactional
    public List<UUID> createLabStudies(CreateLabStudiesCommand cmd) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.require();

        boolean consultationExists = eventRepository.findById(cmd.consultationId())
                .filter(e -> e.getEventType() == ClinicalEventType.CONSULTATION)
                .isPresent();

        if (!consultationExists) {
            throw new ClinicalDomainException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Consultation " + cmd.consultationId() + " not found");
        }

        var record = recordRepository.findById(cmd.recordId())
                .orElseThrow(() -> new ClinicalDomainException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Medical record " + cmd.recordId() + " not found"));

        String patientName = patientRepository.findById(record.getPatientId())
                .map(p -> p.fullName())
                .orElse(record.getPatientId().toString());
        Instant now = Instant.now();
        List<UUID> studyIds = new ArrayList<>();

        for (int i = 0; i < cmd.studies().size(); i++) {
            var study = cmd.studies().get(i);
            String studyIdemKey = cmd.idempotencyKey() + "-LAB-" + i;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("studyId", study.studyId().toString());
            payload.put("consultationId", cmd.consultationId().toString());
            payload.put("studyType", study.studyType());
            payload.put("priority", study.priority() != null ? study.priority() : "ROUTINE");
            payload.put("status", "PENDING");
            if (study.instructions() != null) payload.put("instructions", study.instructions());

            ClinicalEvent event = ClinicalEvent.builder()
                    .eventId(study.studyId())
                    .recordId(cmd.recordId())
                    .eventType(ClinicalEventType.LAB_ORDER)
                    .branchId(branchId)
                    .performedByStaffId(staffId)
                    .idempotencyKey(studyIdemKey)
                    .payload(payload)
                    .build();

            ClinicalEvent saved = eventStore.append(event);
            studyIds.add(saved.getEventId());

            PendingLabStudy pending = new PendingLabStudy(
                    study.studyId(), saved.getEventId(), cmd.recordId(),
                    record.getPatientId(), patientName,
                    cmd.consultationId(), study.studyType(),
                    study.priority() != null ? study.priority() : "ROUTINE",
                    study.instructions(), now, staffId, branchId
            );
            pendingRepository.save(pending);
        }

        log.info("Lab studies created: count={}, consultation={}, record={}",
                studyIds.size(), cmd.consultationId(), cmd.recordId());
        return studyIds;
    }

    @Transactional
    public UUID recordLabResult(RecordLabResultCommand cmd) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.require();

        PendingLabStudy study = pendingRepository.findById(cmd.studyId())
                .orElseThrow(() -> new ClinicalDomainException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lab study " + cmd.studyId() + " not found"));

        if (study.getStatus() != StudyStatus.PENDING && study.getStatus() != StudyStatus.IN_PROGRESS) {
            throw new ClinicalDomainException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Lab study " + cmd.studyId() + " is already " + study.getStatus());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resultId", cmd.resultId().toString());
        payload.put("studyId", cmd.studyId().toString());
        payload.put("studyType", study.getStudyType());
        payload.put("resultText", cmd.resultText());

        ClinicalEvent event = ClinicalEvent.builder()
                .eventId(cmd.resultId())
                .recordId(study.getRecordId())
                .eventType(ClinicalEventType.LAB_RESULT)
                .branchId(branchId)
                .performedByStaffId(staffId)
                .idempotencyKey(cmd.idempotencyKey())
                .payload(payload)
                .build();

        ClinicalEvent saved = eventStore.append(event);

        study.recordResult(cmd.resultText(), staffId);
        pendingRepository.save(study);

        log.info("Lab result recorded: resultId={}, studyId={}", cmd.resultId(), cmd.studyId());
        return saved.getEventId();
    }
}
