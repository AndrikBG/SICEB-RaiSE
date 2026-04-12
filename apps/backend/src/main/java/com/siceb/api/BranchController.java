package com.siceb.api;

import com.siceb.platform.branch.command.RegisterBranchCommand;
import com.siceb.platform.branch.command.UpdateBranchCommand;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.entity.BranchOnboardingStep;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchOnboardingStepRepository;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.branch.service.BranchContextService;
import com.siceb.platform.branch.service.BranchOnboardingOrchestrator;
import com.siceb.platform.branch.service.BranchRegistrationService;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.shared.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BranchController {

    private final BranchRegistrationService registrationService;
    private final BranchContextService contextService;
    private final BranchOnboardingOrchestrator onboardingOrchestrator;
    private final BranchRepository branchRepository;
    private final BranchOnboardingStepRepository onboardingStepRepository;
    private final AuthorizationService authorizationService;

    public BranchController(
            BranchRegistrationService registrationService,
            BranchContextService contextService,
            BranchOnboardingOrchestrator onboardingOrchestrator,
            BranchRepository branchRepository,
            BranchOnboardingStepRepository onboardingStepRepository,
            AuthorizationService authorizationService) {
        this.registrationService = registrationService;
        this.contextService = contextService;
        this.onboardingOrchestrator = onboardingOrchestrator;
        this.branchRepository = branchRepository;
        this.onboardingStepRepository = onboardingStepRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAuthority('branch:manage')")
    public ResponseEntity<BranchResponse> registerBranch(@Valid @RequestBody RegisterBranchRequest request) {
        var cmd = new RegisterBranchCommand(
                request.name(), request.address(), request.phone(), request.email(),
                request.openingTime(), request.closingTime(), request.branchCode());

        Branch branch = registrationService.registerBranch(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(BranchResponse.from(branch));
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAuthority('branch:read')")
    public ResponseEntity<List<BranchResponse>> listBranches(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        List<Branch> branches = includeInactive
                ? branchRepository.findAll()
                : branchRepository.findByActiveTrue();

        List<BranchResponse> response = branches.stream().map(BranchResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('branch:read')")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @PutMapping("/branches/{branchId}")
    @PreAuthorize("hasAuthority('branch:manage')")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        var cmd = new UpdateBranchCommand(
                request.name(), request.address(), request.phone(),
                request.email(), request.openingTime(), request.closingTime());

        Branch branch = registrationService.updateBranch(branchId, cmd);
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @PostMapping("/branches/{branchId}/deactivate")
    @PreAuthorize("hasAuthority('branch:manage')")
    public ResponseEntity<BranchResponse> deactivateBranch(@PathVariable UUID branchId) {
        Branch branch = registrationService.deactivateBranch(branchId);
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @GetMapping("/branches/{branchId}/onboarding")
    @PreAuthorize("hasAuthority('branch:manage')")
    public ResponseEntity<OnboardingStatusResponse> getOnboardingStatus(@PathVariable UUID branchId) {
        List<BranchOnboardingStep> steps = onboardingStepRepository.findByBranchIdOrderByCreatedAtAsc(branchId);

        List<OnboardingStepResponse> stepResponses = steps.stream()
                .map(s -> new OnboardingStepResponse(s.getStepName(), s.getStatus(), s.getCompletedAt()))
                .toList();

        long completed = steps.stream().filter(BranchOnboardingStep::isCompleted).count();
        String overall = completed == steps.size() ? "COMPLETED"
                : steps.stream().anyMatch(s -> "FAILED".equals(s.getStatus())) ? "FAILED"
                : "IN_PROGRESS";

        return ResponseEntity.ok(new OnboardingStatusResponse(
                branchId, stepResponses, overall, (int) completed, steps.size()));
    }

    @PostMapping("/session/branch")
    public ResponseEntity<BranchSwitchResponse> switchBranch(
            @Valid @RequestBody SwitchBranchRequest request) {

        UUID userId = authorizationService.currentPrincipal().userId();
        BranchContextService.BranchSwitchResult result = contextService.switchBranch(userId, request.branchId());

        return ResponseEntity.ok(new BranchSwitchResponse(
                result.accessToken(), result.branchId(), result.branchName()));
    }

    // ---- Request DTOs ----

    public record RegisterBranchRequest(
            @NotBlank String name,
            String address,
            String phone,
            String email,
            LocalTime openingTime,
            LocalTime closingTime,
            String branchCode
    ) {}

    public record UpdateBranchRequest(
            String name,
            String address,
            String phone,
            String email,
            LocalTime openingTime,
            LocalTime closingTime
    ) {}

    public record SwitchBranchRequest(
            @NotNull UUID branchId
    ) {}

    // ---- Response DTOs ----

    public record BranchResponse(
            UUID branchId,
            String name,
            String address,
            String phone,
            String email,
            LocalTime openingTime,
            LocalTime closingTime,
            String branchCode,
            boolean isActive,
            boolean onboardingComplete
    ) {
        static BranchResponse from(Branch b) {
            return new BranchResponse(
                    b.getBranchId(), b.getName(), b.getAddress(), b.getPhone(), b.getEmail(),
                    b.getOpeningTime(), b.getClosingTime(), b.getBranchCode(),
                    b.isActive(), b.isOnboardingComplete());
        }
    }

    public record OnboardingStatusResponse(
            UUID branchId,
            List<OnboardingStepResponse> steps,
            String overallStatus,
            int completedSteps,
            int totalSteps
    ) {}

    public record OnboardingStepResponse(
            String name,
            String status,
            java.time.Instant completedAt
    ) {}

    public record BranchSwitchResponse(
            String accessToken,
            UUID branchId,
            String branchName
    ) {}
}
