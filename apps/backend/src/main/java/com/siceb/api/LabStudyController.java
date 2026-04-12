package com.siceb.api;

import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.laboratory.command.CreateLabStudiesCommand;
import com.siceb.domain.laboratory.command.RecordLabResultCommand;
import com.siceb.domain.laboratory.model.PendingLabStudy;
import com.siceb.domain.laboratory.model.StudyStatus;
import com.siceb.domain.laboratory.repository.PendingLabStudyRepository;
import com.siceb.domain.laboratory.service.LabStudyCommandHandler;
import com.siceb.platform.branch.TenantContext;
import com.siceb.shared.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Laboratory", description = "Laboratory study orders and results")
public class LabStudyController {

    private final LabStudyCommandHandler labHandler;
    private final PendingLabStudyRepository pendingRepository;

    public LabStudyController(LabStudyCommandHandler labHandler,
                               PendingLabStudyRepository pendingRepository) {
        this.labHandler = labHandler;
        this.pendingRepository = pendingRepository;
    }

    @PostMapping("/consultations/{consultationId}/lab-studies")
    @PreAuthorize("@auth.check('lab:order', true)")
    @Operation(summary = "Create lab study orders from a consultation (US-038)")
    public ResponseEntity<Map<String, Object>> createLabStudies(
            @PathVariable UUID consultationId,
            @Valid @RequestBody CreateLabStudiesCommand cmd) {
        if (!consultationId.equals(cmd.consultationId())) {
            throw new ClinicalDomainException(
                    ErrorCode.VALIDATION_FAILED,
                    "consultationId in path must match consultationId in body");
        }
        List<UUID> studyIds = labHandler.createLabStudies(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "studyIds", studyIds.stream().map(UUID::toString).toList()
        ));
    }

    @PostMapping("/lab-studies/{studyId}/results")
    @PreAuthorize("@auth.check('lab:result')")
    @Operation(summary = "Record lab study results (US-041)")
    public ResponseEntity<Map<String, String>> recordLabResult(
            @PathVariable UUID studyId,
            @Valid @RequestBody RecordLabResultCommand cmd) {
        UUID resultId = labHandler.recordLabResult(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "resultId", resultId.toString()
        ));
    }

    @GetMapping("/lab-studies/pending")
    @PreAuthorize("@auth.check('lab:read')")
    @Operation(summary = "List pending lab studies for current branch (US-040)")
    public ResponseEntity<List<PendingLabStudy>> getPendingStudies(
            @RequestParam(required = false) String status) {
        UUID branchId = TenantContext.require();

        List<PendingLabStudy> studies;
        if (status != null) {
            studies = pendingRepository.findByBranchIdAndStatusOrderByRequestedAtAsc(
                    branchId, StudyStatus.valueOf(status.toUpperCase()));
        } else {
            studies = pendingRepository.findByBranchIdAndStatusInOrderByRequestedAtAsc(
                    branchId, List.of(StudyStatus.PENDING, StudyStatus.IN_PROGRESS));
        }
        return ResponseEntity.ok(studies);
    }
}
