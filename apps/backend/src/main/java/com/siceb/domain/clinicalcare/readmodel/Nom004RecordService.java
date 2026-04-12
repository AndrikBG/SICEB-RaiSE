package com.siceb.domain.clinicalcare.readmodel;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.model.MedicalRecord;
import com.siceb.domain.clinicalcare.model.Patient;
import com.siceb.domain.clinicalcare.repository.ClinicalEventRepository;
import com.siceb.domain.clinicalcare.repository.MedicalRecordRepository;
import com.siceb.domain.clinicalcare.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Nom004RecordView — structured projection organizing events into
 * NOM-004-SSA3-2012 mandatory sections (CRN-31).
 * Sections: identification, clinical notes, diagnostics, lab summaries, prescriptions, attachments.
 */
@Service
@Transactional(readOnly = true)
public class Nom004RecordService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository recordRepository;
    private final ClinicalEventRepository eventRepository;

    public Nom004RecordService(PatientRepository patientRepository,
                                MedicalRecordRepository recordRepository,
                                ClinicalEventRepository eventRepository) {
        this.patientRepository = patientRepository;
        this.recordRepository = recordRepository;
        this.eventRepository = eventRepository;
    }

    public Nom004Record buildRecord(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + patientId));

        MedicalRecord record = recordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new NoSuchElementException("Medical record not found for patient: " + patientId));

        List<ClinicalEvent> events = eventRepository.findByRecordIdOrderByOccurredAtAsc(record.getRecordId());

        Map<String, Object> identification = new LinkedHashMap<>();
        identification.put("patientId", patient.getPatientId().toString());
        identification.put("fullName", patient.fullName());
        identification.put("dateOfBirth", patient.getDateOfBirth().toString());
        identification.put("gender", patient.getGender().name());
        identification.put("patientType", patient.getPatientType().name());
        identification.put("curp", patient.getCurp());
        identification.put("phone", patient.getPhone());
        if (patient.isMinor()) {
            identification.put("guardianName", patient.getGuardianName());
            identification.put("guardianRelationship", patient.getGuardianRelationship());
        }

        List<Map<String, Object>> clinicalNotes = new ArrayList<>();
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        List<Map<String, Object>> labSummaries = new ArrayList<>();
        List<Map<String, Object>> prescriptions = new ArrayList<>();
        List<Map<String, Object>> attachments = new ArrayList<>();

        for (ClinicalEvent event : events) {
            Map<String, Object> entry = new LinkedHashMap<>(event.getPayload());
            entry.put("occurredAt", event.getOccurredAt().toString());
            entry.put("performedBy", event.getPerformedByStaffId().toString());

            switch (event.getEventType()) {
                case CONSULTATION -> {
                    clinicalNotes.add(entry);
                    Map<String, Object> diag = new LinkedHashMap<>();
                    diag.put("diagnosis", event.getPayload().get("diagnosis"));
                    diag.put("diagnosisCode", event.getPayload().get("diagnosisCode"));
                    diag.put("date", event.getOccurredAt().toString());
                    diagnostics.add(diag);
                }
                case PRESCRIPTION -> prescriptions.add(entry);
                case LAB_ORDER, LAB_RESULT -> labSummaries.add(entry);
                case ATTACHMENT -> attachments.add(entry);
                default -> {}
            }
        }

        return new Nom004Record(
                record.getRecordId(),
                identification,
                clinicalNotes,
                diagnostics,
                labSummaries,
                prescriptions,
                attachments
        );
    }

    public record Nom004Record(
            UUID recordId,
            Map<String, Object> identification,
            List<Map<String, Object>> clinicalNotes,
            List<Map<String, Object>> diagnostics,
            List<Map<String, Object>> labSummaries,
            List<Map<String, Object>> prescriptions,
            List<Map<String, Object>> attachments
    ) {}
}
