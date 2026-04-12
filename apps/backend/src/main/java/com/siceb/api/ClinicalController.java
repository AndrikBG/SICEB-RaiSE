package com.siceb.api;

import com.siceb.domain.clinicalcare.command.AddConsultationCommand;
import com.siceb.domain.clinicalcare.command.CreatePatientCommand;
import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.clinicalcare.readmodel.*;
import com.siceb.domain.clinicalcare.service.ConsultationService;
import com.siceb.domain.clinicalcare.service.PatientService;
import com.siceb.domain.prescriptions.command.CreatePrescriptionCommand;
import com.siceb.domain.prescriptions.service.PrescriptionCommandHandler;
import com.siceb.platform.branch.TenantContext;
import com.siceb.shared.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Clinical Care", description = "Patient registration, consultations, prescriptions, and clinical records")
public class ClinicalController {

    private final PatientService patientService;
    private final ConsultationService consultationService;
    private final PrescriptionCommandHandler prescriptionHandler;
    private final PatientSearchRepository searchRepository;
    private final ClinicalTimelineService timelineService;
    private final Nom004RecordService nom004Service;
    private final ClinicalEventProjector eventProjector;

    public ClinicalController(PatientService patientService,
                               ConsultationService consultationService,
                               PrescriptionCommandHandler prescriptionHandler,
                               PatientSearchRepository searchRepository,
                               ClinicalTimelineService timelineService,
                               Nom004RecordService nom004Service,
                               ClinicalEventProjector eventProjector) {
        this.patientService = patientService;
        this.consultationService = consultationService;
        this.prescriptionHandler = prescriptionHandler;
        this.searchRepository = searchRepository;
        this.timelineService = timelineService;
        this.nom004Service = nom004Service;
        this.eventProjector = eventProjector;
    }

    @PostMapping("/patients")
    @PreAuthorize("@auth.check('patient:create')")
    @Operation(summary = "Create a new patient with medical record (US-019, CRN-37)")
    public ResponseEntity<Map<String, String>> createPatient(@Valid @RequestBody CreatePatientCommand cmd) {
        var result = patientService.createPatient(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "patientId", result.patientId().toString(),
                "recordId", result.recordId().toString()
        ));
    }

    @PostMapping("/consultations")
    @PreAuthorize("@auth.check('consultation:create', true)")
    @Operation(summary = "Add a consultation to a medical record (US-025, USA-02)")
    public ResponseEntity<Map<String, String>> addConsultation(@Valid @RequestBody AddConsultationCommand cmd) {
        UUID consultationId = consultationService.addConsultation(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "consultationId", consultationId.toString()
        ));
    }

    @PostMapping("/consultations/{consultationId}/prescriptions")
    @PreAuthorize("@auth.check('prescription:create', true)")
    @Operation(summary = "Create a prescription within a consultation (US-031)")
    public ResponseEntity<Map<String, String>> createPrescription(
            @PathVariable UUID consultationId,
            @Valid @RequestBody CreatePrescriptionCommand cmd) {
        if (!consultationId.equals(cmd.consultationId())) {
            throw new ClinicalDomainException(
                    ErrorCode.VALIDATION_FAILED,
                    "consultationId in path must match consultationId in body");
        }
        UUID prescriptionId = prescriptionHandler.createPrescription(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "prescriptionId", prescriptionId.toString()
        ));
    }

    @GetMapping("/patients/search")
    @PreAuthorize("@auth.check('patient:search')")
    @Operation(summary = "Search patients with sub-1s target (PER-03)")
    public ResponseEntity<Page<PatientSearchEntry>> searchPatients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate dateOfBirth,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID branchId = TenantContext.require();
        Page<PatientSearchEntry> results = searchRepository.search(
                branchId, q, dateOfBirth, type, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/patients/duplicates")
    @PreAuthorize("@auth.check('patient:read')")
    @Operation(summary = "Find potential duplicate patients by full name and date of birth (US-019)")
    public ResponseEntity<java.util.List<Map<String, String>>> findDuplicatePatients(
            @RequestParam String firstName,
            @RequestParam String paternalSurname,
            @RequestParam(required = false) String maternalSurname,
            @RequestParam LocalDate dateOfBirth) {
        var duplicates = patientService.findDuplicateCandidates(firstName, paternalSurname, maternalSurname, dateOfBirth)
                .stream()
                .map(p -> Map.of(
                        "patientId", p.getPatientId().toString(),
                        "fullName", p.fullName(),
                        "dateOfBirth", p.getDateOfBirth().toString()
                ))
                .toList();
        return ResponseEntity.ok(duplicates);
    }

    @GetMapping("/patients/{patientId}/timeline")
    @PreAuthorize("@auth.check('record:read')")
    @Operation(summary = "Get clinical timeline for a patient (US-027)")
    public ResponseEntity<Page<ClinicalTimelineService.TimelineEntry>> getTimeline(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var searchEntry = searchRepository.findById(patientId).orElse(null);
        if (searchEntry == null || searchEntry.getRecordId() == null) {
            return ResponseEntity.notFound().build();
        }
        var timeline = timelineService.getTimelinePaginated(
                searchEntry.getRecordId(), PageRequest.of(page, size));
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/patients/{patientId}/nom004")
    @PreAuthorize("@auth.check('record:read')")
    @Operation(summary = "Get NOM-004-SSA3-2012 structured record (CRN-31)")
    public ResponseEntity<Nom004RecordService.Nom004Record> getNom004Record(@PathVariable UUID patientId) {
        var record = nom004Service.buildRecord(patientId);
        return ResponseEntity.ok(record);
    }
}
