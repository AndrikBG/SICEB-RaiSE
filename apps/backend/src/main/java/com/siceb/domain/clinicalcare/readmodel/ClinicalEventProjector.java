package com.siceb.domain.clinicalcare.readmodel;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.model.MedicalRecord;
import com.siceb.domain.clinicalcare.model.Patient;
import com.siceb.domain.clinicalcare.repository.MedicalRecordRepository;
import com.siceb.domain.clinicalcare.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects clinical events into read models (T2.5.4).
 * Called after each successful event append to keep read models synchronized.
 */
@Service
public class ClinicalEventProjector {

    private static final Logger log = LoggerFactory.getLogger(ClinicalEventProjector.class);

    private final PatientSearchRepository searchRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public ClinicalEventProjector(PatientSearchRepository searchRepository,
                                   PatientRepository patientRepository,
                                   MedicalRecordRepository medicalRecordRepository) {
        this.searchRepository = searchRepository;
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Transactional
    public void project(ClinicalEvent event) {
        switch (event.getEventType()) {
            case RECORD_CREATED -> projectRecordCreated(event);
            case CONSULTATION -> projectConsultation(event);
            default -> log.debug("No projection needed for event type: {}", event.getEventType());
        }
    }

    private void projectRecordCreated(ClinicalEvent event) {
        String patientIdStr = (String) event.getPayload().get("patientId");
        if (patientIdStr == null) return;

        java.util.UUID patientId = java.util.UUID.fromString(patientIdStr);
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) return;

        String recordIdStr = (String) event.getPayload().get("recordId");
        java.util.UUID recordId = recordIdStr != null ? java.util.UUID.fromString(recordIdStr) : null;

        PatientSearchEntry entry = searchRepository.findById(patientId)
                .orElseGet(() -> new PatientSearchEntry(
                        patient.getPatientId(),
                        patient.fullName(),
                        patient.getDateOfBirth(),
                        patient.getPatientType().name(),
                        patient.getGender().name(),
                        patient.getPhone(),
                        patient.getCurp(),
                        patient.getCredentialNumber(),
                        patient.getProfileStatus().name(),
                        patient.getBranchId(),
                        recordId
                ));
        searchRepository.save(entry);
        log.debug("Projected patient to search view: {}", patient.getPatientId());
    }

    private void projectConsultation(ClinicalEvent event) {
        String patientIdStr = findPatientIdForRecord(event);
        if (patientIdStr == null) return;

        java.util.UUID patientId = java.util.UUID.fromString(patientIdStr);
        searchRepository.findById(patientId).ifPresent(entry -> {
            entry.updateLastVisit(event.getOccurredAt());
            searchRepository.save(entry);
            log.debug("Updated last visit for patient: {}", patientId);
        });
    }

    private String findPatientIdForRecord(ClinicalEvent event) {
        Object pid = event.getPayload().get("patientId");
        if (pid != null) return pid.toString();
        return medicalRecordRepository.findById(event.getRecordId())
                .map(MedicalRecord::getPatientId)
                .map(java.util.UUID::toString)
                .orElse(null);
    }
}
