package com.siceb.domain.clinicalcare.service;

import com.siceb.domain.clinicalcare.command.CreatePatientCommand;
import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.clinicalcare.model.*;
import com.siceb.domain.clinicalcare.repository.MedicalRecordRepository;
import com.siceb.domain.clinicalcare.repository.PatientRepository;
import com.siceb.platform.branch.TenantContext;
import com.siceb.platform.iam.StaffContext;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * PatientAggregate — handles patient creation with business invariants:
 * global uniqueness (CRN-37), guardian validation for minors (US-023),
 * patient type classification with discount (US-020).
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private static final int MINOR_AGE_THRESHOLD = 18;

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalEventStore eventStore;

    public PatientService(PatientRepository patientRepository,
                          MedicalRecordRepository medicalRecordRepository,
                          ClinicalEventStore eventStore) {
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.eventStore = eventStore;
    }

    /**
     * Creates a patient and their medical record, emitting a RECORD_CREATED event.
     * Returns a result DTO with patientId and recordId.
     */
    @Transactional
    public CreatePatientResult createPatient(CreatePatientCommand cmd) {
        UUID branchId = TenantContext.require();
        UUID staffId = StaffContext.require();

        if (patientRepository.existsByPatientId(cmd.patientId())) {
            throw new ClinicalDomainException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Patient with id " + cmd.patientId() + " already exists");
        }

        validateGuardian(cmd);
        validateCredential(cmd);
        validatePhone(cmd.phone());

        Patient patient = Patient.builder()
                .patientId(cmd.patientId())
                .firstName(cmd.firstName())
                .paternalSurname(cmd.paternalSurname())
                .maternalSurname(cmd.maternalSurname())
                .dateOfBirth(cmd.dateOfBirth())
                .gender(cmd.gender())
                .phone(cmd.phone())
                .curp(cmd.curp())
                .patientType(cmd.patientType())
                .credentialNumber(cmd.credentialNumber())
                .guardianName(cmd.guardianName())
                .guardianRelationship(cmd.guardianRelationship())
                .guardianPhone(cmd.guardianPhone())
                .guardianIdConfirmed(cmd.guardianIdConfirmed())
                .dataConsentGiven(cmd.dataConsentGiven())
                .specialCase(cmd.specialCase())
                .specialCaseNotes(cmd.specialCaseNotes())
                .branchId(branchId)
                .createdByStaffId(staffId)
                .build();

        patientRepository.save(patient);

        UUID recordId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord(recordId, patient.getPatientId(), branchId, staffId);
        medicalRecordRepository.save(record);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientId", patient.getPatientId().toString());
        payload.put("fullName", patient.fullName());
        payload.put("dateOfBirth", patient.getDateOfBirth().toString());
        payload.put("patientType", patient.getPatientType().name());
        payload.put("recordId", recordId.toString());

        ClinicalEvent event = ClinicalEvent.builder()
                .eventId(UUID.randomUUID())
                .recordId(recordId)
                .eventType(ClinicalEventType.RECORD_CREATED)
                .branchId(branchId)
                .performedByStaffId(staffId)
                .idempotencyKey(cmd.idempotencyKey())
                .payload(payload)
                .build();

        eventStore.append(event);

        log.info("Created patient={} with record={} at branch={}", patient.getPatientId(), recordId, branchId);
        return new CreatePatientResult(patient.getPatientId(), recordId);
    }

    public List<Patient> findDuplicateCandidates(String firstName, String paternalSurname,
                                                  String maternalSurname, LocalDate dob) {
        String name = firstName + " " + paternalSurname + " " + (maternalSurname != null ? maternalSurname : "");
        return patientRepository.findDuplicateCandidates(name.trim(), dob);
    }

    private void validateGuardian(CreatePatientCommand cmd) {
        int age = Period.between(cmd.dateOfBirth(), LocalDate.now()).getYears();
        if (age < MINOR_AGE_THRESHOLD && !cmd.specialCase()) {
            if (cmd.guardianName() == null || cmd.guardianName().isBlank()) {
                throw new ClinicalDomainException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Patient under " + MINOR_AGE_THRESHOLD + " requires a registered guardian (US-023)");
            }
            if (cmd.guardianRelationship() == null || cmd.guardianRelationship().isBlank()) {
                throw new ClinicalDomainException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Guardian relationship is required for minors (US-023)");
            }
            if (!cmd.guardianIdConfirmed()) {
                throw new ClinicalDomainException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Guardian official ID must be confirmed for minors (US-023)");
            }
        }
    }

    private void validateCredential(CreatePatientCommand cmd) {
        if (cmd.patientType() == PatientType.STUDENT || cmd.patientType() == PatientType.WORKER) {
            if (cmd.credentialNumber() == null || cmd.credentialNumber().isBlank()) {
                throw new ClinicalDomainException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Credential/ID number is required for " + cmd.patientType() + " patients (US-020)");
            }
        }
    }

    private void validatePhone(String phone) {
        if (phone != null && !phone.isBlank() && !phone.matches("\\d{10,15}")) {
            throw new ClinicalDomainException(ErrorCode.VALIDATION_FAILED,
                    "Phone must be 10-15 digits (US-019)");
        }
    }

    public record CreatePatientResult(UUID patientId, UUID recordId) {}
}
