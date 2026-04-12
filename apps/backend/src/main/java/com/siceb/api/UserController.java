package com.siceb.api;

import com.siceb.platform.iam.entity.MedicalStaff;
import com.siceb.platform.iam.entity.Permission;
import com.siceb.platform.iam.entity.User;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.platform.iam.service.UserManagementService;
import com.siceb.platform.iam.service.UserManagementService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User CRUD, role & branch assignment (US-001)")
public class UserController {

    private final UserManagementService userManagementService;
    private final AuthorizationService authorizationService;

    public UserController(UserManagementService userManagementService,
                          AuthorizationService authorizationService) {
        this.userManagementService = userManagementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    @PreAuthorize("@auth.check('user:read')")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = userManagementService.listUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@auth.check('user:read')")
    @Operation(summary = "Get user details")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable UUID userId) {
        User user = userManagementService.getUser(userId);
        MedicalStaff staff = userManagementService.getMedicalStaff(userId).orElse(null);
        return ResponseEntity.ok(UserDetailResponse.from(user, staff));
    }

    @PostMapping
    @PreAuthorize("@auth.check('user:manage')")
    @Operation(summary = "Create a new user (US-001)")
    public ResponseEntity<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequestDto dto) {
        CreateUserRequest request = new CreateUserRequest(
                dto.username(),
                dto.email(),
                dto.fullName(),
                dto.roleId(),
                dto.primaryBranchId(),
                dto.branchAssignments(),
                dto.temporaryPassword(),
                dto.medicalStaff() != null ? new MedicalStaffInfo(
                        dto.medicalStaff().specialty(),
                        dto.medicalStaff().residencyLevel(),
                        dto.medicalStaff().canPrescribeControlled(),
                        dto.medicalStaff().professionalLicense(),
                        dto.medicalStaff().supervisorStaffId()
                ) : null
        );

        UserCreationResult result = userManagementService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserDetailResponse.from(result.user(), result.medicalStaff()));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@auth.check('user:manage')")
    @Operation(summary = "Update user profile and assignments")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequestDto dto) {
        UpdateUserRequest request = new UpdateUserRequest(
                dto.fullName(), dto.email(), dto.roleId(), dto.branchAssignments());
        User updated = userManagementService.updateUser(userId, request);
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("@auth.check('user:manage')")
    @Operation(summary = "Deactivate user — revokes all tokens (US-001)")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        UUID performedBy = authorizationService.currentPrincipal().userId();
        userManagementService.deactivateUser(userId, performedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/activate")
    @PreAuthorize("@auth.check('user:manage')")
    @Operation(summary = "Reactivate a deactivated user")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        userManagementService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("@auth.check('user:manage')")
    @Operation(summary = "Reset user password to temporary — forces change on next login")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID userId) {
        userManagementService.resetPassword(userId);
        return ResponseEntity.noContent().build();
    }

    // ---- DTOs ----

    public record CreateUserRequestDto(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank String fullName,
            @NotNull UUID roleId,
            @NotNull UUID primaryBranchId,
            Set<UUID> branchAssignments,
            String temporaryPassword,
            MedicalStaffDto medicalStaff
    ) {}

    public record MedicalStaffDto(
            @NotBlank String specialty,
            String residencyLevel,
            boolean canPrescribeControlled,
            String professionalLicense,
            UUID supervisorStaffId
    ) {}

    public record UpdateUserRequestDto(
            String fullName,
            @Email String email,
            UUID roleId,
            Set<UUID> branchAssignments
    ) {}

    public record UserResponse(
            String id,
            String username,
            String fullName,
            String email,
            String role,
            boolean active,
            boolean mustChangePassword,
            String primaryBranchId,
            Set<String> branchAssignments
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getUserId().toString(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getRole().getName(),
                    u.isActive(),
                    u.isMustChangePassword(),
                    u.getBranchId().toString(),
                    u.getAssignedBranches().stream()
                            .map(b -> b.getBranchId().toString())
                            .collect(Collectors.toSet())
            );
        }
    }

    public record UserDetailResponse(
            String id,
            String username,
            String fullName,
            String email,
            String role,
            String roleId,
            Set<String> permissions,
            boolean active,
            boolean mustChangePassword,
            String primaryBranchId,
            Set<String> branchAssignments,
            MedicalStaffResponse medicalStaff
    ) {
        public static UserDetailResponse from(User u, MedicalStaff staff) {
            return new UserDetailResponse(
                    u.getUserId().toString(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getRole().getName(),
                    u.getRole().getRoleId().toString(),
                    u.getRole().getPermissions().stream()
                            .map(Permission::getKey).collect(Collectors.toSet()),
                    u.isActive(),
                    u.isMustChangePassword(),
                    u.getBranchId().toString(),
                    u.getAssignedBranches().stream()
                            .map(b -> b.getBranchId().toString())
                            .collect(Collectors.toSet()),
                    staff != null ? MedicalStaffResponse.from(staff) : null
            );
        }
    }

    public record MedicalStaffResponse(
            String staffId,
            String specialty,
            String residencyLevel,
            boolean canPrescribeControlled,
            String professionalLicense,
            String supervisorStaffId
    ) {
        public static MedicalStaffResponse from(MedicalStaff s) {
            return new MedicalStaffResponse(
                    s.getStaffId().toString(),
                    s.getSpecialty(),
                    s.getResidencyLevel(),
                    s.isCanPrescribeControlled(),
                    s.getProfessionalLicense(),
                    s.getSupervisor() != null ? s.getSupervisor().getStaffId().toString() : null
            );
        }
    }
}
